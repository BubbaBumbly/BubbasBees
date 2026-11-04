#version 150

// Gentle pulse on bright texels only: ~2.5 s per cycle, each texel at its own phase, never fully off.
float bb_hash(vec2 p) {
    return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453);
}

vec3 bb_twinkle(vec3 color, vec2 texel, float gameTime) {
    float ticks = gameTime * 24000.0;
    float lum = max(color.r, max(color.g, color.b));
    float pulse = 0.82 + 0.18 * sin(ticks * 6.2831853 / 50.0 + bb_hash(texel) * 6.2831853);
    return color * mix(1.0, pulse, smoothstep(0.45, 0.8, lum));
}

uniform sampler2D Sampler0;   // sky tile, opaque
uniform sampler2D Sampler1;   // near stars, transparent
uniform sampler2D Sampler2;   // the bee's own texture: only its alpha is used, as the shape mask
uniform vec2 ScreenSize;
uniform float GameTime;

in vec2 texCoord0;

out vec4 fragColor;

void main() {
    // Same cut as entityCutout: no stars where the bee texture is see-through (around antennae, legs, stinger)
    if (texture(Sampler2, texCoord0).a < 0.1) discard;
    // Fixed to the screen: about 3 sky tiles from the bottom of the window to the top, whatever its size
    vec2 uv = gl_FragCoord.xy / ScreenSize.y * 3.0;
    vec2 size = vec2(textureSize(Sampler0, 0));
    vec3 sky = bb_twinkle(texture(Sampler0, uv).rgb, floor(uv * size), GameTime);
    // A nearer star layer, larger and drifting slowly, for depth
    vec2 uv2 = uv * 0.6 + vec2(GameTime * 2.0, GameTime);
    vec4 near = texture(Sampler1, uv2);
    vec3 nearC = bb_twinkle(near.rgb, floor(uv2 * size) + 17.0, GameTime);
    fragColor = vec4(mix(sky, nearC, near.a), 1.0);
}
