// @flow

import * as THREE from 'three';
import { AR } from 'expo-ar';

export default class BackgroundTexture extends THREE.Texture {
  constructor(renderer: THREE.WebGLRenderer) {
    super();
    this.initCameraTexture(renderer);
  }

  initCameraTexture = async renderer => {
    const cameraTexture = await AR.getCameraTextureAsync();
    const properties = renderer.properties.get(this);
    properties.__webglInit = true;
    properties.__webglTexture = new WebGLTexture(cameraTexture);
  };
}
