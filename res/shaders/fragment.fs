#version 330

in vec2 tex_coord;
in vec4 hitmod;

out vec4 frag_colour;

uniform sampler2D image;

void main()
{
    frag_colour = texture(image, tex_coord) * hitmod;
}