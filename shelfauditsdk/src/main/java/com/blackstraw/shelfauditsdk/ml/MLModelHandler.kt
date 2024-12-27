package com.blackstraw.shelfauditsdk.ml

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.ops.CastOp
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer

object MLModelHandler {


    private const val INPUT_MEAN = 0f
    private const val INPUT_STANDARD_DEVIATION = 255f
    private val INPUT_IMAGE_TYPE = DataType.FLOAT32
    private val OUTPUT_IMAGE_TYPE = DataType.FLOAT32
    private const val IOU_THRESHOLD = 0.3F


    private lateinit var imageProcessorDetection: ImageProcessor
    private lateinit var imageProcessorClassification: ImageProcessor

    private lateinit var interpreterDetection: Interpreter
    private lateinit var interpreterClassification: Interpreter

    private var tensorWidthDetection = 0
    private var tensorHeightDetection = 0
    private var tensorWidthClip = 0
    private var tensorHeightClip = 0
    private var tensorInputChannelClip = 0
    private var numChannelDetection = 0
    private var numElementsDetection = 0
    private var numElementsClip = 0

    private lateinit var outputDetection: TensorBuffer
    private lateinit var outputClassification: TensorBuffer


    fun init(context: Context) {
        MLModelInitializer.initModels(context)
        initializeInterpreters()
        processTensorShapes()
        imageProcessorDetection = getImageProcessor(tensorWidthDetection, tensorHeightDetection)
        imageProcessorClassification = getImageProcessor(tensorWidthClip, tensorHeightClip)
        generateOutputFormats()
    }

    /**
     * Function to initialize the TensorFlow Lite interpreters.
     * The function will initialize the detection and classification model interpreters.
     */
    private fun initializeInterpreters() {
        this.interpreterDetection = MLModelInitializer.getDetectionModelInterpreter()!!
        this.interpreterClassification = MLModelInitializer.getClassificationModelInterpreter()!!
    }

    /**
     * Processes the tensor shapes for detection and classification models.
     * This function retrieves the input and output tensor shapes for both models
     * and assigns the appropriate dimensions to the respective variables.
     */
    private fun processTensorShapes() {
        val inputShapeDetection = interpreterDetection.getInputTensor(0)?.shape()
        val inputShapeClassification = interpreterClassification.getInputTensor(0)?.shape()
        val outputShapeDetection = interpreterDetection.getOutputTensor(0)?.shape()
        val outputShapeClassification = interpreterClassification.getOutputTensor(0)?.shape()


        if (inputShapeDetection != null) {
            tensorWidthDetection = inputShapeDetection[1]
            tensorHeightDetection = inputShapeDetection[2]

            // If in case input shape is in format of [1, 3, ..., ...]
            if (inputShapeDetection[1] == 3) {
                tensorWidthDetection = inputShapeDetection[2]
                tensorHeightDetection = inputShapeDetection[3]
            }
        }

        if (inputShapeClassification != null) {
            tensorWidthClip = inputShapeClassification[1]
            tensorHeightClip = inputShapeClassification[2]
            tensorInputChannelClip = inputShapeClassification[3]

            // If in case input shape is in format of [1, 3, ..., ...]
            if (inputShapeClassification[1] == 3) {
                tensorWidthClip = inputShapeClassification[2]
                tensorHeightClip = inputShapeClassification[3]
                tensorInputChannelClip = inputShapeClassification[1]
            }
        }

        if (outputShapeDetection != null) {
            numChannelDetection = outputShapeDetection[1]
            numElementsDetection = outputShapeDetection[2]
        }

        if (outputShapeClassification != null) {
            numElementsClip = outputShapeClassification[1]
        }
    }


    /**
     * Function to get the image processor for the classification model.
     * The function will return an ImageProcessor object.
     * The function will take the target width, target height, and input standard deviation as input parameters.
     * The function will create an ImageProcessor object with ResizeOp and NormalizeOp.
     *
     * @param targetWidth The target width for resizing the image.
     * @param targetHeight The target height for resizing the image.
     * @param inputStandardDeviation The standard deviation for normalizing the image.
     * @return An ImageProcessor object configured with ResizeOp and NormalizeOp.
     */
    private fun getImageProcessor(
        targetWidth: Int,
        targetHeight: Int,
        inputStandardDeviation: Float = INPUT_STANDARD_DEVIATION
    ): ImageProcessor {
        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(targetHeight, targetWidth, ResizeOp.ResizeMethod.NEAREST_NEIGHBOR))
            .add(NormalizeOp(INPUT_MEAN, inputStandardDeviation))
            .add(CastOp(INPUT_IMAGE_TYPE))
            .build()
        return imageProcessor
    }

    /**
     * Function to generate the output formats for detection and classification models.
     * The function will create TensorBuffer objects for detection and classification models.
     */
    private fun generateOutputFormats() {
        this.outputDetection = TensorBuffer.createFixedSize(intArrayOf(1, numChannelDetection, numElementsDetection), OUTPUT_IMAGE_TYPE)
        this.outputClassification = TensorBuffer.createFixedSize(intArrayOf(1, numElementsClip), OUTPUT_IMAGE_TYPE)
    }


    /**
     * Function to get the detection output for a given bitmap.
     * The function will take a bitmap as input and return the detection output as a float array.
     * The function will load the bitmap into a TensorImage object.
     * The function will process the image using the image processor for detection.
     * The function will run the interpreter for detection model.
     * The function will return the output as a float array.
     *
     * @param bitmap The input bitmap for detection.
     * @return The detection output as a list of bounding boxes.
     * @see BoundingBox
     */
    fun getDetectionOutputForBitmap(bitmap: Bitmap): List<BoundingBox> {
        var output: List<BoundingBox> = emptyList()
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, tensorWidthDetection, tensorHeightDetection, true)
        val inputTensor = TensorImage(INPUT_IMAGE_TYPE)
        inputTensor.load(scaledBitmap)
        val processedImage = imageProcessorDetection.process(inputTensor)
        interpreterDetection.run(processedImage.buffer, outputDetection.buffer)
        val outputFloatArray = outputDetection.floatArray
        output = bestBox(outputFloatArray) ?: emptyList()
        return output
    }

    /**
     * Function to get the classification output for a given bitmap.
     * The function will take a bitmap as input and return the classification output as a float array.
     * The function will load the bitmap into a TensorImage object.
     * The function will process the image using the image processor for classification.
     * The function will run the interpreter for classification model.
     * The function will return the output as a float array.
     *
     * @param bitmap The input bitmap for classification.
     * @return The classification output as a float array.
     */
    fun getClassificationOutputForBitmap(bitmap: Bitmap): FloatArray? {
        val inputTensor = TensorImage(INPUT_IMAGE_TYPE)
        inputTensor.load(bitmap)
        val processedImage = imageProcessorClassification.process(inputTensor)
        interpreterClassification.run(processedImage.buffer, outputClassification.buffer)
        return outputClassification.floatArray
    }

    /**
     * Function to get the best bounding boxes for a given float array.
     * The function will take a float array as input and return the best bounding boxes.
     * The function will loop through the float array and get the best bounding boxes.
     * The function will return the best bounding boxes as a list.
     *
     * @param array The input float array for bounding boxes.
     * @return The best bounding boxes as a list.
     */
    private fun bestBox(array: FloatArray) : List<BoundingBox> {

        val boundingBoxes = mutableListOf<BoundingBox>()
        val detectionConfidenceCurrentThreshold = 0.4F

        for (c in 0 until numElementsDetection) {
            var clsName = "PRODUCT"
            var maxConf = detectionConfidenceCurrentThreshold
            var maxIdx = -1
            var isROI = false
            // The confidence score is now at the last channel, so we loop through the channels accordingly
            var j = (numChannelDetection-1) // Start at the confidence score channel (the 11th channel is 10 in 0-based index)
            var arrayIdx = c + numElementsDetection * j
            while (j < numChannelDetection) {
                if (array[arrayIdx] > maxConf) {
                    maxConf = array[arrayIdx]
                    maxIdx = j - (numChannelDetection-1)
                }
                j++
                arrayIdx += numElementsDetection
            }

            for(k in 4 until (numChannelDetection-1)){
                val roiValue = array[c + numElementsDetection * k]
                if(roiValue > detectionConfidenceCurrentThreshold) {
                    maxConf = roiValue
                    isROI = true
                }
            }

            // Update class name based on ROI detection
            if (isROI) {
                clsName = "ROI"
            }

            // Continue only if the confidence is greater than the threshold
            if (maxConf > detectionConfidenceCurrentThreshold && !isROI) {

                val cx = array[c] // Channel 0: X center
                val cy = array[c + numElementsDetection] // Channel 1: Y center
                val w = array[c + numElementsDetection * 2] // Channel 2: Width
                val h = array[c + numElementsDetection * 3] // Channel 3: Height


                // Ignore R1-R6 channels since they are not required for bounding box calculations
                val x1 = cx - (w / 2F)
                val y1 = cy - (h / 2F)
                val x2 = cx + (w / 2F)
                val y2 = cy + (h / 2F)

                // Check if the bounding box is within valid range (0 to 1 for normalized coordinates)
                if (x1 < 0F || x1 > 1F) continue
                if (y1 < 0F || y1 > 1F) continue
                if (x2 < 0F || x2 > 1F) continue
                if (y2 < 0F || y2 > 1F) continue

                // Add the bounding box to the list
                boundingBoxes.add(
                    BoundingBox(
                        x1 = x1, y1 = y1, x2 = x2, y2 = y2,
                        cx = cx, cy = cy, w = w, h = h,
                        cnf = maxConf,
                        cls = maxIdx,
                        clsName = clsName
                    )
                )

            }
        }

        // If no valid bounding boxes, return null
        if (boundingBoxes.isEmpty()) return emptyList()

        // Apply Non-Maximum Suppression (NMS) to remove duplicate/overlapping boxes
        return  applyNMS(boundingBoxes)
    }


   /**
 * Apply Non-Maximum Suppression (NMS) to remove duplicate/overlapping boxes.
 * The function will take a list of bounding boxes as input and return a list of selected bounding boxes.
 * The function will sort the bounding boxes by confidence score in descending order.
 * The function will iterate through the sorted bounding boxes and select the boxes with the highest confidence score.
 * The function will calculate the Intersection over Union (IoU) between the selected box and the remaining boxes.
 * The function will remove the boxes with IoU greater than the threshold.
 * The function will return the selected bounding boxes as a list.
 *
 * @param boxes The list of bounding boxes to process.
 * @return A list of selected bounding boxes after applying NMS.
 */
   fun applyNMS(boxes: List<BoundingBox>) : MutableList<BoundingBox> {
       val sortedBoxes = boxes.sortedByDescending { it.cnf }.toMutableList()
       val selectedBoxes = mutableListOf<BoundingBox>()

       val currentThresholdIOU = IOU_THRESHOLD

       while(sortedBoxes.isNotEmpty()) {
           val first = sortedBoxes.first()
           selectedBoxes.add(first)
           sortedBoxes.remove(first)

           val iterator = sortedBoxes.iterator()
           while (iterator.hasNext()) {
               val nextBox = iterator.next()
               val iou = calculateIoU(first, nextBox)
               if (iou >= currentThresholdIOU) {
                   iterator.remove()
               }
           }
       }

       return selectedBoxes
    }



     /**
     * Function to calculate Intersection over Union (IoU) between two bounding boxes.
     * The function will take two bounding boxes as input and return the IoU value.
     * The function will calculate the intersection area and the union area between the two boxes.
     * The function will return the IoU value as a float.
     *
     * @param box1 The first bounding box.
     * @param box2 The second bounding box.
     * @return The IoU value as a float.
     */
     fun calculateIoU(box1: BoundingBox, box2: BoundingBox): Float {
        val x1 = maxOf(box1.x1, box2.x1)
        val y1 = maxOf(box1.y1, box2.y1)
        val x2 = minOf(box1.x2, box2.x2)
        val y2 = minOf(box1.y2, box2.y2)
        val intersectionArea = maxOf(0F, x2 - x1) * maxOf(0F, y2 - y1)
        val box1Area = box1.w * box1.h
        val box2Area = box2.w * box2.h
        return intersectionArea / (box1Area + box2Area - intersectionArea)
    }



}