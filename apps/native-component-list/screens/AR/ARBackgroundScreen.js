import React from 'react';
import {
  AR,
  GLView,
} from 'expo';
import { findNodeHandle, StyleSheet, View } from 'react-native';
import { mat4 } from 'gl-matrix';

import { initShaderProgram, checkGLError } from './ARUtils';

export default class ARBackgroundScreen extends React.Component {
  static title = 'AR Background';

  render() {
    return (
      <View style={{ flex: 1 }}>
        <GLView
          ref={ref => (this.glView = ref)}
          style={StyleSheet.absoluteFill}
          onContextCreate={this.onGLContextCreate}
        />
      </View>
    );
  }

  createBackgroundGLProgram = gl => {
    const program = initShaderProgram(gl, `
      precision highp float;

      attribute vec2 aTextureCoord;
      varying vec2 uv;

      void main() {
        uv = aTextureCoord;
        gl_Position = vec4(1.0 - 2.0 * aTextureCoord, -1, 1);
      }
    `, `
      precision highp float;
      
      uniform sampler2D uSampler;
      varying vec2 uv;
      
      void main() {
        gl_FragColor = texture2D(uSampler, vec2(1) - uv);
      }
    `);

    return {
      program,
      attribLocations: {
        textureCoord: gl.getAttribLocation(program, 'position'),
      },
      uniformLocations: {
        uSampler: gl.getUniformLocation(program, 'uSampler'),
      },
    };
  }

  createBackgroundBuffers = gl => {
    // vertices would be placed outside the visible box
    // therefore we'll only need one triangle for each face of cube
    //            /\
    //           /  \
    //          /    \
    //         /______\
    //        /| visi |\
    //       / |  ble | \
    //      /  |______|__\
    //     /       ___/
    //    /    ___/
    //   / ___/        
    //  /_/
    //
    const verticesCoords = [ // these vertices would be transformed by vertexShader into:
      -2,  0, //  5,  1
       0, -2, //  1,  5
       2,  2, // -3, -3
    ]; // that would allow us to render only one triangle that would contain whole visible screen with texture
    const positionBuffer = gl.createBuffer();
    gl.bindBuffer(gl.ARRAY_BUFFER, positionBuffer);
    gl.bufferData(gl.ARRAY_BUFFER, new Float32Array(verticesCoords), gl.STATIC_DRAW);

    return {
      positionBuffer,
    };
  }

  createCameraStream = async gl => {
    const {
      program,
      attribLocations,
      uniformLocations,
    } = this.createBackgroundGLProgram(gl);

    const {
      positionBuffer,
    } = this.createBackgroundBuffers(gl);

    const capturedCameraTexture = await AR.getCameraTextureAsync();
    const texture = new WebGLTexture(capturedCameraTexture);
    
    return {
      draw: () => {
        gl.useProgram(program);

        gl.bindBuffer(gl.ARRAY_BUFFER, positionBuffer);
        gl.enableVertexAttribArray(attribLocations.textureCoord);
        gl.vertexAttribPointer(attribLocations.textureCoord, 2, gl.FLOAT, false, 0, 0);

        gl.uniform1i(uniformLocations.uSampler, 0);

        // cleanup element array buffer
        gl.bindBuffer(gl.ELEMENT_ARRAY_BUFFER, null);

        gl.activeTexture(gl.TEXTURE0);
        gl.bindTexture(gl.TEXTURE_2D, texture);
        gl.drawArrays(gl.TRIANGLES, 0, 3); // we have 3 vertices
        checkGLError(gl, 'draw camera');
      },
    };
  };

  onGLContextCreate = async gl => {
    this.gl = gl;
    
    await AR.startAsync(findNodeHandle(this.glView.nativeRef), AR.TrackingConfigurations.World);
    
    this.cameraStream = await this.createCameraStream(this.gl);

    let then = 0;
    // Render loop
    const loop = (time) => {
      const now = time * 0.001;
      const deltaTime = now - then;
      then = now;

      // Clear
      gl.clearColor(0.2, 0.5, 0.5, 1);
      gl.clearDepth(1.0);
      gl.enable(gl.DEPTH_TEST);
      gl.depthFunc(gl.LEQUAL);
      gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT);
      
      // Draw camera stream
      if (this.cameraStream) {
        this.cameraStream.draw(deltaTime);
      }
      
      // Submit frame
      gl.endFrameEXP();
      requestAnimationFrame(loop);
    };
    requestAnimationFrame(loop);
  };
}
