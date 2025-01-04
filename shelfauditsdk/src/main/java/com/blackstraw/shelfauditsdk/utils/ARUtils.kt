package com.blackstraw.shelfauditsdk.utils

import android.graphics.Bitmap
import android.graphics.YuvImage
import android.media.Image
import android.util.Log
import com.blackstraw.shelfauditsdk.ml.BoundingBox
import com.google.android.filament.Box
import com.google.android.filament.Engine
import com.google.android.filament.MaterialInstance
import com.google.ar.core.Anchor
import com.google.ar.core.Frame
import com.google.ar.core.Pose
import com.google.ar.core.exceptions.NotYetAvailableException
import io.github.sceneview.geometries.Geometry
import io.github.sceneview.geometries.Shape
import io.github.sceneview.math.Position
import io.github.sceneview.math.Position2
import io.github.sceneview.math.Rotation
import io.github.sceneview.node.GeometryNode
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer




fun createCustomGeometryNode(
    engine: Engine,
    shape: Shape,
    materialInstance: MaterialInstance,
    position: Position = Position(),
    rotation: Rotation = Rotation()
): GeometryNode {
    return GeometryNode(engine, shape, materialInstance).apply {
        transform(
            position = position,
            rotation = rotation
        )
    }
}


fun createGeometryNodeFromBoundingBox(
    engine: Engine,
    boundingBox: BoundingBox,
    materialInstance: MaterialInstance?
): GeometryNode {
    val geometry = create2DModelFromBoundingBox(boundingBox, engine)
    return GeometryNode(engine, geometry, materialInstance)
}

fun create2DModelFromBoundingBox(boundingBox: BoundingBox, engine: Engine): Shape {
    // Convert BoundingBox to Position2 list

    val scaleFactor = 1.0f

    val points: List<Position2> = listOf(
        Position2(boundingBox.x1.times(scaleFactor), boundingBox.y1.times(scaleFactor)),
        Position2(boundingBox.x2.times(scaleFactor), boundingBox.y1.times(scaleFactor)),
        Position2(boundingBox.x2.times(scaleFactor), boundingBox.y2.times(scaleFactor)),
        Position2(boundingBox.x1.times(scaleFactor), boundingBox.y2.times(scaleFactor))
    )
    // Create Shape using polygonPath function
    return Shape.Builder()
        .polygonPath(points)
        .build(engine)
}



fun getCurrentFrameAsBitmap(frame: Frame): Bitmap? {
    var image: Image? = null
    for (i in 0..4) { // Retry up to 5 times
        try {
            image = frame.acquireCameraImage()
            break
        } catch (e: NotYetAvailableException) {
            Log.d("ARSceneView", "Image is not yet available. Retrying...", e)
            Thread.sleep(50) // Wait for 50ms before retrying
        }
    }

    if (image == null) {
        return null // Return null if the image is still not available
    }

    val planes: Array<Image.Plane> = image.planes
    val yBuffer: ByteBuffer = planes[0].buffer
    val uBuffer: ByteBuffer = planes[1].buffer
    val vBuffer: ByteBuffer = planes[2].buffer

    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()

    val nv21 = ByteArray(ySize + uSize + vSize)

    yBuffer.get(nv21, 0, ySize)
    val uBytes = ByteArray(uSize)
    val vBytes = ByteArray(vSize)
    uBuffer.get(uBytes, 0, uSize)
    vBuffer.get(vBytes, 0, vSize)

    for (i in 0 until uSize) {
        nv21[ySize + i * 2] = vBytes[i]
        nv21[ySize + i * 2 + 1] = uBytes[i]
    }

    val yuvImage = YuvImage(nv21, android.graphics.ImageFormat.NV21, image.width, image.height, null)
    val out = ByteArrayOutputStream()
    yuvImage.compressToJpeg(android.graphics.Rect(0, 0, image.width, image.height), 100, out)
    val jpegData = out.toByteArray()

    val bitmap = android.graphics.BitmapFactory.decodeByteArray(jpegData, 0, jpegData.size)

    image.close()
    return bitmap.rotate()
}

fun Bitmap.rotate(value: Float = 90F) : Bitmap {
    val matrix = android.graphics.Matrix()
    matrix.postRotate(value)
    return Bitmap.createBitmap(this, 0, 0, this.width, this.height, matrix, true)
}

fun convertBoundingBoxToBox(boundingBox: BoundingBox): Box {
    val centerX = (boundingBox.x1 + boundingBox.x2) / 2
    val centerY = (boundingBox.y1 + boundingBox.y2) / 2
    val centerZ = 0.0f // Assuming a 2D bounding box, set Z to 0

    val halfExtentX = (boundingBox.x2 - boundingBox.x1) / 2
    val halfExtentY = (boundingBox.y2 - boundingBox.y1) / 2
    val halfExtentZ = 0.0f // Assuming a 2D bounding box, set Z to 0

    return Box(centerX, centerY, centerZ, halfExtentX, halfExtentY, halfExtentZ)
}


fun convertBoundingBoxToARPoints(session: com.google.ar.core.Session, frame: Frame, boundingBox: BoundingBox, maxDepth: Float): ARPoints? {
    val corners = mutableListOf<Anchor>()

    // Convert the bounding box corners to AR points
    val points = listOf(
        floatArrayOf(boundingBox.x1, boundingBox.y1),
        floatArrayOf(boundingBox.x2, boundingBox.y1),
        floatArrayOf(boundingBox.x1, boundingBox.y2),
        floatArrayOf(boundingBox.x2, boundingBox.y2)
    )

    for (point in points) {
        val hitResult = frame.hitTest(point[0], point[1])
        if (hitResult.isNotEmpty()) {
            val hitPose = hitResult[0].hitPose
            val poseWithMaxDepth = Pose(
                floatArrayOf(hitPose.tx(), hitPose.ty(), maxDepth),
                hitPose.rotationQuaternion
            )
            val anchor = session.createAnchor(poseWithMaxDepth)
            corners.add(anchor)
        }
    }

    // Calculate the center point
    val centerX = (boundingBox.x1 + boundingBox.x2) / 2
    val centerY = (boundingBox.y1 + boundingBox.y2) / 2
    val centerHitResult = frame.hitTest(centerX, centerY)
    if (centerHitResult.isNotEmpty()) {
        val centerHitPose = centerHitResult[0].hitPose
        val centerPoseWithMaxDepth = Pose(
            floatArrayOf(centerHitPose.tx(), centerHitPose.ty(), maxDepth),
            centerHitPose.rotationQuaternion
        )
        val centerAnchor = session.createAnchor(centerPoseWithMaxDepth)
        return ARPoints(corners, centerAnchor)
    }

    return null
}

fun createBoxFromBoundingBox(boundingBox: BoundingBox): Box {
    val centerX = boundingBox.cx
    val centerY = boundingBox.cy
    val centerZ = 0.0f // Assuming a 2D bounding box, set Z to 0 or any appropriate value

    val halfExtentX = boundingBox.w / 2
    val halfExtentY = boundingBox.h / 2
    val halfExtentZ = 0.0f // Assuming a 2D bounding box, set Z to 0 or any appropriate value

    return Box(centerX, centerY, centerZ, halfExtentX, halfExtentY, halfExtentZ)
}

fun convertBoundingBoxToAnchors(session: com.google.ar.core.Session, frame: Frame, boundingBox: BoundingBox): List<Anchor> {
    val anchors = mutableListOf<Anchor>()

    // List of points in the bounding box
    val points = listOf(
        floatArrayOf(boundingBox.x1, boundingBox.y1), // top-left
        floatArrayOf(boundingBox.x2, boundingBox.y1), // top-right
        floatArrayOf(boundingBox.x1, boundingBox.y2), // bottom-left
        floatArrayOf(boundingBox.x2, boundingBox.y2), // bottom-right
        floatArrayOf((boundingBox.x1 + boundingBox.x2) / 2, (boundingBox.y1 + boundingBox.y2) / 2) // center
    )

    // Convert each point to an Anchor
    for (point in points) {
        val hitResult = frame.hitTest(point[0], point[1])
        if (hitResult.isNotEmpty()) {
            val hitPose = hitResult[0].hitPose
            val anchor = session.createAnchor(hitPose)
            anchors.add(anchor)
        }
    }

    return anchors
}

data class ARPoints(
    val corners: List<Anchor>,
    val center: Anchor
)