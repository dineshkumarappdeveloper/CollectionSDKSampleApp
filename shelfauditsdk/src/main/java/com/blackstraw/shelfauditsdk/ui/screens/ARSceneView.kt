package com.blackstraw.shelfauditsdk.ui.screens

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.blackstraw.shelfauditsdk.utils.ARPoints
import com.blackstraw.shelfauditsdk.utils.ShakeDetector
import com.blackstraw.shelfauditsdk.utils.getCurrentFrameAsBitmap
import com.blackstraw.shelfauditsdk.utils.isLowLighting
import com.blackstraw.shelfauditsdk.utils.isTilted
import com.blackstraw.shelfauditsdk.utils.isTooClose
import com.blackstraw.shelfauditsdk.utils.isTooFar
import com.blackstraw.shelfauditsdk.viewModels.ARViewModel
import com.google.android.filament.Engine
import com.google.android.filament.IndexBuffer
import com.google.android.filament.VertexBuffer
import com.google.ar.core.Config
import io.github.sceneview.ar.ARScene
import io.github.sceneview.ar.node.ARCameraNode
import io.github.sceneview.ar.rememberARCameraStream
import io.github.sceneview.ar.scene.PlaneRenderer
import io.github.sceneview.rememberCollisionSystem
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberEnvironmentLoader
import io.github.sceneview.rememberMainLightNode
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNodes
import io.github.sceneview.rememberRenderer
import io.github.sceneview.rememberScene
import io.github.sceneview.rememberView
import java.nio.ByteBuffer
import java.nio.ByteOrder


@Composable
fun ARSceneView() {
    Box(modifier = Modifier.fillMaxSize()) {

        val context = LocalContext.current
        val engine = rememberEngine()
        val view = rememberView(engine)
        val renderer = rememberRenderer(engine)
        val scene = rememberScene(engine)
        val modelLoader = rememberModelLoader(engine)
        val environmentLoader = rememberEnvironmentLoader(engine)
        val collisionSystem = rememberCollisionSystem(view)
        val materialLoader = rememberMaterialLoader(engine)
        val shakeDetector = remember { ShakeDetector(context) }
        val nodes = rememberNodes()

        val arViewModel = remember { ARViewModel() }
        val boundingBoxList  = arViewModel.listBoundingBoxes.collectAsState(emptyList())


        val modelNode =  remember {
            mutableStateOf<ARCameraNode?>(null)
        }

        val arPointList = mutableListOf<ARPoints>()



        ARScene(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            view = view,
            childNodes = nodes,
            renderer = renderer,
            scene = scene,
            modelLoader = modelLoader,
            materialLoader = materialLoader,
            environmentLoader = environmentLoader,
            collisionSystem = collisionSystem,
            sessionCameraConfig = null,
            isOpaque = true,
            // Always add a direct light source since it is required for shadowing.
            // We highly recommend adding an [IndirectLight] as well.
            mainLightNode = rememberMainLightNode(engine) {
                intensity = 100_000.0f
            },
            sessionFeatures = setOf(),
            // The config must be one returned by [Session.getSupportedCameraConfigs].
            // Provides details of a camera configuration such as size of the CPU image and GPU texture.
            // Configures the session and verifies that the enabled features in the specified session config
            // are supported with the currently set camera config.
            sessionConfiguration = { session, config ->
                config.depthMode =
                    when (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                        true -> Config.DepthMode.AUTOMATIC
                        else -> Config.DepthMode.DISABLED
                    }
                config.instantPlacementMode = Config.InstantPlacementMode.LOCAL_Y_UP
                config.lightEstimationMode =  Config.LightEstimationMode.ENVIRONMENTAL_HDR
            },
            planeRenderer = false,
            // The [ARCameraStream] to render the camera texture.
            // Use it to control if the occlusion should be enabled or disabled.
            cameraStream = rememberARCameraStream(materialLoader),
            // The session is ready to be accessed.
            onSessionCreated = { session ->
                Log.d("ARSceneView", "Session created"+session.cameraConfig.fpsRange)
            },
            // The session has been resumed.
            onSessionResumed = { session ->
            },
            // The session has been paused
            onSessionPaused = { session ->
            },
            // Updates of the state of the ARCore system.
            // This includes: receiving a new camera frame, updating the location of the device, updating
            // the location of tracking anchors, updating detected planes, etc.
            // This call may update the pose of all created anchors and detected planes. The set of updated
            // objects is accessible through [Frame.getUpdatedTrackables].
            // Invoked once per [Frame] immediately before the Scene is updated.
            onSessionUpdated = { session, updatedFrame ->
                val bitmap = getCurrentFrameAsBitmap(updatedFrame)
                if (bitmap != null) {
                    val boundingBoxes = boundingBoxList.value



                    val materialInstance = materialLoader.createColorInstance(
                        color = Color.Blue,
                        metallic = 0.5f,
                        roughness = 1.0f,
                        reflectance = 0.0f
                    )

//                    for (boundingBox in boundingBoxes) {
//
//                        val shape = createGeometryNodeFromBoundingBox(
//                            engine,
//                            boundingBox,
//                            null
//                        )
//
//                        val aabb = createBoxFromBoundingBox(boundingBox)
//
////                         val renderableManager: RenderableManager.Builder.() -> Unit = {
////                            boundingBox(aabb)
////                        }
//
//                        val faceVertexBuffer = createVertexBuffer(engine)
//                        val faceIndexBuffer = createIndexBuffer(engine)
//
//
//                     val renderableManager: RenderableManager.Builder.() -> Unit = {
//                        boundingBox(aabb)
//                        geometry(0, RenderableManager.PrimitiveType.LINES, faceVertexBuffer, faceIndexBuffer, 0, 468)
//                        material(0, materialInstance)
//                    }
//
//                        //  val anchor = convertBoundingBoxToAnchors(session, updatedFrame, boundingBox)
//                        val geometryNode = GeometryNode(
//                            engine = engine,
//                            geometry = shape.geometry,
//                            materialInstance = materialInstance,
//                            builderApply = renderableManager
//                        )
//
//                        nodes.add(geometryNode)
//
//
//                    }
//
//                    arViewModel.getBoundingBoxes(bitmap)

                }

                // Check for low lighting
                if (isLowLighting(updatedFrame)) {
                  // context.showToastMessage("Low lighting")
                }

                // Check for tilt
                if (isTilted(updatedFrame, 30.0f)) {
                    //context.showToastMessage("Tilted")
                }


                arPointList.forEach { arPoints ->
                    arPoints.corners.forEach { anchor ->
                        if (isTooFar(anchor, 5.0f)) {
                            context.showToastMessage("Anchor too far")
                        }
                        if (isTooClose(anchor, 0.5f)) {
                           context.showToastMessage("Anchor too close")
                        }
                    }
                }


            },
            // Invoked when an ARCore error occurred.
            // Registers a callback to be invoked when the ARCore Session cannot be initialized because
            // ARCore is not available on the device or the camera permission has been denied.
            onSessionFailed = { exception ->
            },
            // Listen for camera tracking failure.
            // The reason that [Camera.getTrackingState] is [TrackingState.PAUSED] or `null` if it is
            // [TrackingState.TRACKING]
            onTrackingFailureChanged = { trackingFailureReason ->
            },
        )


    }
}

fun Context.showToastMessage(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

fun createVertexBuffer(engine: Engine): VertexBuffer {
    // Define vertex data (e.g., positions, normals, etc.)
    val vertexData = floatArrayOf(
        // Positions (x, y, z)
        -1.0f, -1.0f, 0.0f,
         1.0f, -1.0f, 0.0f,
         1.0f,  1.0f, 0.0f,
        -1.0f,  1.0f, 0.0f
    )

    // Create a ByteBuffer to hold the vertex data
    val vertexBuffer = ByteBuffer.allocateDirect(vertexData.size * 4).order(ByteOrder.nativeOrder())
    vertexBuffer.asFloatBuffer().put(vertexData)

    // Create the VertexBuffer
    return VertexBuffer.Builder()
        .vertexCount(4)
        .bufferCount(1)
        .attribute(VertexBuffer.VertexAttribute.POSITION, 0, VertexBuffer.AttributeType.FLOAT3, 0, 12)
        .build(engine).apply {
            setBufferAt(engine, 0, vertexBuffer)
        }
}

fun createIndexBuffer(engine: Engine): IndexBuffer {
    // Define index data
    val indexData = shortArrayOf(
        0, 1, 2,
        2, 3, 0
    )

    // Create a ByteBuffer to hold the index data
    val indexBuffer = ByteBuffer.allocateDirect(indexData.size * 2).order(ByteOrder.nativeOrder())
    indexBuffer.asShortBuffer().put(indexData)

    // Create the IndexBuffer
    return IndexBuffer.Builder()
        .indexCount(6)
        .bufferType(IndexBuffer.Builder.IndexType.USHORT)
        .build(engine).apply {
            setBuffer(engine, indexBuffer)
        }
}