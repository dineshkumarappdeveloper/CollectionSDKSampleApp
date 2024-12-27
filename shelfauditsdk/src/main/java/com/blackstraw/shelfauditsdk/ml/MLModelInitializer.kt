package com.blackstraw.shelfauditsdk.ml

import android.content.Context
import com.blackstraw.shelfauditsdk.utils.loadMappedFileFromPath
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import java.io.File
import java.io.FileNotFoundException

object MLModelInitializer {

    private lateinit var interpreterOptions: Interpreter.Options
    private var interpreterDetection: Interpreter? = null
    private var interpreterClassification: Interpreter? = null


    /**
     * Function to initialize the TensorFlow Lite models.
     * The function will initialize the detection and classification models.
     * The function will also set the interpreter options.
     * The function will catch the FileNotFoundException if the model file is not found.
     */
    fun initModels(context: Context) {
        try {
            val modelPathDetection = "path/to/detection/model"
            val modelPathClassification = "path/to/classification/model"

            this.interpreterOptions = getDelegate()
            this.interpreterDetection = createDetectionModelInterpreter(context, modelPathDetection)
            this.interpreterClassification = createClassificationModelInterpreter(context, modelPathClassification)
        } catch (e: FileNotFoundException) {
            println("Model file not found: ${e.message}")
        }
    }

    /**
     * This function is used to get the delegate for the interpreter.
     * If the device supports GPU delegate, it will use the GPU delegate.
     * Otherwise, it will use the CPU delegate.
     * The number of threads is set to 4.
     */
    private fun getDelegate(): Interpreter.Options {
        val compatList = CompatibilityList()
        val options = Interpreter.Options().apply{
            if(compatList.isDelegateSupportedOnThisDevice){
                val delegateOptions = compatList.bestOptionsForThisDevice
                this.addDelegate(GpuDelegate(delegateOptions))
            } else {
                this.setNumThreads(4)
            }
        }
        return options
    }

    /**
     * Function to get the interpreter for the detection model.
     *
     * @param modelPath The file path to the detection model.
     * @return An instance of the TensorFlow Lite Interpreter for the detection model.
     */
    private fun createDetectionModelInterpreter(context: Context, modelPath: String): Interpreter? {
        val mappedBufferOutput = loadMappedFileFromPath(context, modelPath, "detection.tflite")
        return mappedBufferOutput?.let { Interpreter(it, this.interpreterOptions) }
    }

    /**
     * Function to get the interpreter for the classification model.
     *
     * @param modelPath The file path to the classification model.
     * @return An instance of the TensorFlow Lite Interpreter for the classification model.
     */
    private fun createClassificationModelInterpreter(context: Context, modelPath: String): Interpreter? {
        val mappedBufferOutput = loadMappedFileFromPath(context, modelPath, "classification.tflite")
        return mappedBufferOutput?.let { Interpreter(it, this.interpreterOptions) }
    }

    /**
     * Getter for the detection model interpreter.
     *
     * @return An instance of the TensorFlow Lite Interpreter for the detection model.
     */
    fun getDetectionModelInterpreter(): Interpreter? {
        return this.interpreterDetection
    }

    /**
     * Getter for the classification model interpreter.
     *
     * @return An instance of the TensorFlow Lite Interpreter for the classification model.
     */
    fun getClassificationModelInterpreter(): Interpreter? {
        return this.interpreterClassification
    }

}
