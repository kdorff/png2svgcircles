# png2svgcircles

This Groovy script takes a PNG as input and creates an SVG where
solidly filled areas will be filled with randomly sized
circles of the same color.

This code started its life as [Javascript gist](https://gist.github.com/gouldingken/8d0b7a05b0b0156da3b8) provided by
[gouldingken](https://gist.github.com/gouldingken). 

## Requirements for running

The easiest way to run this is to install [sdkman](https://sdkman.io/)
and then install compatible Java and Groovy packages.
* java 11.0.2-open
* groovy 3.0.14

## Usage (command line options)

```
Usage: png2svgcircles.groovy 
    [-hV] [-d=<doubleOffset>] -i=<inputPngFile> [-m=<minSize>]
    [-n=<numCirclesPerSize>] -o=<outputSvgFile>
    [-r=<numRetriesPerCircle>] [-s=<spacing>] [-x=<maxSize>]
    [-c=<omitColors>]...
Draws an SVG using random circles from a simple PNG
  -i, --input=<inputPngFile>
        The input PNG file
  -o, --output=<outputSvgFile>
        The output SVG file
  -m, --min-size=<minSize>
        The minimum circle size - default is 10
  -x, --max-size=<maxSize>
        The maximum circle size - default is 50
  -s, --spacing=<spacing>
        Minimum space between circles - default is 10
  -n, --num-circles=<numCirclesPerSize>
        Number of circles of each size to try to place -
        default is 100
  -r, --retries=<numRetriesPerCircle>
        Number of retries for placing each circle -
        default is 1000
  -c, --color-skip=<omitColors>
        Colors (argb values) to be omitted. Can be
        included multiple times to omit multiple colors.
  -t, --color-tolerance
        Color tolerance for color matching, in percentage. 
        Range is 0.0 (any color will match) to 1.0 
        (exact color match required). 
        The alpha channel is ignored. Default is 1.0
  -d, --double-circles-offset=<doubleOffset>
        Double the placed circles with each doubled circle
        having the radius reduced by this much.
  -V, --version
        Print version information and exit.
  -h, --help
        Show this help message and exit.
```

## Examples

### Example 1

```bash
groovy png2svgcircles.groovy -i images/26-and-oval.png -o images/26-and-oval.svg -s 5 -m 5 -x 30 -c -1
```

**Sample input file 26-and-oval.png**

![red-star.png](images/26-and-oval.png)

**Example output file 26-and-oval.svg**

![red-star.svg](images/26-and-oval.svg)

### Example 2

```bash
groovy png2svgcircles.groovy -i images/red-star.png -o images/red-star.svg -s 2 -m 5 -x 30 -c 0
```

**Sample input file red-star.png**

![red-star.png](images/red-star.png)

**Example output file red-star.svg**

![red-star.svg](images/red-star.svg)

### Example 3

And an example for [ResinChem Tech](https://www.youtube.com/@ResinChemTech) where we use the tolerance option, `-t`, to better handle images that have non-uniform coloring.

```bash
groovy png2svgcircles.groovy -i images/Chicken_New01.png -o images/Chicken_New01.svg -s 1 -m 2 -x 20 -t 0.5
```

**Sample input file Chicken_New01.png**

![Chicken_New01.png](images/Chicken_New01.png)

**Example output file Chicken_New01.svg**

![Chicken_New01.svg](images/Chicken_New01.svg)
