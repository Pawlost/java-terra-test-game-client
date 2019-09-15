#import "Common/ShaderLib/GLSLCompat.glsllib"
in vec3 inPosition;
in vec2 inTexCoord;

uniform float m_strength;
uniform float m_height;
uniform float m_cylindricalRatio;

uniform float m_aspect;
uniform mat4 g_WorldViewProjectionMatrix;

varying vec3 vUV;
varying vec2 vUVDot;

void main() {
    vec2 pos = inPosition.xy * 2.0 - 1.0;
    gl_Position = vec4(pos, 0.0, 1.0);


    float scaledHeight = m_strength * m_height;
    float cylAspectRatio = m_aspect * m_cylindricalRatio;
    float aspectDiagSq = m_aspect * m_aspect + 1.0;
    float diagSq = scaledHeight * scaledHeight * aspectDiagSq;
    vec2 signedUV = (2.0 * inTexCoord + vec2(-1.0, -1.0));

    float z = 0.5 * sqrt(diagSq + 1.0) + 0.5;
    float ny = (z - 1.0) / (cylAspectRatio * cylAspectRatio + 1.0);

    vUVDot = sqrt(ny) * vec2(cylAspectRatio, 1.0) * signedUV;
    vUV = vec3(0.5, 0.5, 1.0) * z + vec3(-0.5, -0.5, 0.0);
    vUV.xy += inTexCoord;
}