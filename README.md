# png2svgcircles

This Groovy script takes a PNG as input and creates an SVG where
solidly filled areas will be filled with randomly sized
circles of the same color.

This code started its life as [Javascript gist](https://gist.github.com/gouldingken/8d0b7a05b0b0156da3b8) provided by
[gouldingken](https://gist.github.com/gouldingken/8d0b7a05b0b0156da3b8). 

# Requirements for running

The easiest way to run this is to install [sdkman](https://sdkman.io/)
and then install compatible Java and Groovy versions.
* java 11.0.2-open
* groovy 3.0.14

# Examples

## Example 1

```bash
groovy png2svgcircles.groovy -i 26-and-oval.png -o 26-and-oval.svg -s 5 -m 5 -x 30 -c -1
```

**Sample input file 26-and-oval.png**

![red-star.png](26-and-oval.png)

**Example output file 26-and-oval.svg**

![red-star.svg](26-and-oval.svg)

## Example 2

```bash
groovy png2svgcircles.groovy -i red-star.png -o red-star.svg -s 2 -m 5 -x 30 -c 0
```

**Sample input file red-star.png**

![red-star.png](red-star.png)

**Example output file red-star.svg**

![red-star.svg](red-star.svg)
