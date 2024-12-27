package com.blackstraw.shelfauditsdk.ui.components

import androidx.compose.runtime.Composable

@Composable
fun CustomToolbar(toolbarType: TOOLBAR_TYPE) {



}

enum class TOOLBAR_TYPE {
    PRIMARY,
    SECONDARY,
    CAMERA_NON_RECORDING,
    CAMERA_RECORDING,
}