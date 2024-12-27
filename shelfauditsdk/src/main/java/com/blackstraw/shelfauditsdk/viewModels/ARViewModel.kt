package com.blackstraw.shelfauditsdk.viewModels

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blackstraw.shelfauditsdk.ml.BoundingBox
import com.blackstraw.shelfauditsdk.ml.MLModelHandler
import io.github.sceneview.node.Node
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ARViewModel : ViewModel() {

    private var _listNodes = mutableListOf<Node>()
    val listNodes: List<Node> get() = _listNodes
    private var isRunning = false

    private val _listBoundingBoxes = MutableStateFlow<List<BoundingBox>>(emptyList())
    val listBoundingBoxes: StateFlow<List<BoundingBox>> get() = _listBoundingBoxes

    init {
        _listNodes = mutableListOf()
        removeAllNodes()
    }

    private fun removeAllNodes() {
        _listNodes.clear()
    }

    fun addNode(node: Node) {
        _listNodes.add(node)
    }

    fun removeNode(node: Node) {
        _listNodes.remove(node)
    }

    fun getBoundingBoxes(bitmap: Bitmap) = viewModelScope.launch(Dispatchers.IO) {
        if (isRunning) return@launch
        isRunning = true
        try {
            MLModelHandler.getDetectionOutputForBitmap(bitmap).apply {
                _listBoundingBoxes.value = this
            }
        } catch (e: OutOfMemoryError) {
            // Ignore the function if OutOfMemoryError is thrown
        } finally {
            isRunning = false
        }
    }




}