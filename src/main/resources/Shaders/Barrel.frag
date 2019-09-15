#import "Common/ShaderLib/GLSLCompat.glsllib"

uniform sampler2D m_Texture;

varying vec3 vUV;
varying vec2 vUVDot;

void main(void)
{
    vec3 uv = dot(vUVDot, vUVDot) * vec3(-0.5, -0.5, -1.0) + vUV;
    gl_FragColor = texture2DProj(m_Texture, uv);
}