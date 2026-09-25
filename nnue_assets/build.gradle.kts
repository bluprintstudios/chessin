plugins {
    id("com.android.asset-pack")
}

assetPack {
    packName = "nnue_assets"
    dynamicDelivery {
        deliveryType = "install-time"
    }
}
