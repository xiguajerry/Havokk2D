#version 330

in vec3 vertices;
in vec2 in_tex;

out vec2 tex_coord;
out vec4 hitmod;

uniform mat4 projection;

uniform mat4 frame;

uniform vec4 hit;

void main()
{
    gl_Position = projection * vec4(vertices, 1.0);
    tex_coord = (frame * vec4(in_tex, 0, 1)).xy;
    hitmod = hit;
}