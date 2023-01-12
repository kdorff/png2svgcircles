#!/usr/bin/env groovy
@Grab('org.jfree:org.jfree.svg:5.0.3')
@Grab('com.github.jai-imageio:jai-imageio-core:1.4.0')
@Grab('info.picocli:picocli-groovy:4.7.0')

import org.jfree.svg.*
import picocli.CommandLine
import static picocli.CommandLine.*
import java.awt.Color
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import java.util.concurrent.Callable
import groovy.transform.AutoClone

/**
 *
 * This Groovy script takes a PNG as input and creates an SVG where
 * solidly filled areas will be filled with randomly sized
 * circles of the same color.
 *
 * See the README.md for more details.
 *
 * TODO
 * ---------------
 * <none>
 *
 * DONE
 * ---------------
 * * Command line options using Picocli
 * * Circles can go off the left and top borders. Fix that.
 * * Option -c to omit making circles with specific argb color values
 *   This can be added multiple times to omit multiple colors.
 * * Simplified code to not need to 'makeCircles' before placing them,
 *   just make circles from maxSize to minSize.
 */

@Command(name = 'svgCircles', mixinStandardHelpOptions = true, version = '0.1',
        description = 'Draws an SVG using random circles from a simple PNG')
class png2svgcircles implements Callable<Integer> {
    @Option(names = ["-i", "--input"], required = true, description = "The input PNG file")
    File inputPngFile

    @Option(names = ["-o", "--output"], required = true, description = "The output SVG file")
    File outputSvgFile

    @Option(names = ["-s", "--spacing"], description = "Minimum space between circles - default is 10")
    int spacing = 10

    @Option(names = ["-n", "--num-circles"], description = "Number of circles of each size to try to place - default is 100")
    int numCirclesPerSize = 100

    @Option(names = ["-r", "--retries"], description = "Number of retries for placing each circle - default is 1000")
    int numRetriesPerCircle = 1000

    @Option(names = ["-m", "--min-size"], description = "The minimum circle size - default is 10")
    int minSize = 10

    @Option(names = ["-x", "--max-size"], description = "The maximum circle size - default is 50")
    int maxSize = 50

    @Option(names = ["-d", "--double-circles-offset"], description = "Double the placed circles with each doubled circle having the radius reduced by this much.")
    Double doubleOffset = null

    @Option(names = ["-c", "--color-skip"], description = "Colors (argb values) to be omitted.")
    List<Integer> omitColors = []

    Random random = new Random()

    /**
     * The input image's pixels, in ARGB format.
     */
    int[] inputPixels = null

    /**
     * The input image's width in pixels.
     */
    Integer canvasWidth

    /**
     * The input image's height in pixels.
     */
    Integer  canvasHeight

    /**
     * List of FillCircle's that HAVE been placed.
     */
    def placedCircles = []

    /**
     * Class to represent a circle to try to place / placed circles.
     */
    @AutoClone 
    class FillCircle {
        double r

        int color
        Map<String, Integer> colorMap

        int x
        int y

        FillCircle(int r) {
            this.r = r
        }

        void setColor(int argb) {
            color = argb
            colorMap = splitArgb(argb)
        }

        String toString() {
            return "FillCircle x=${x}, y=${y}, r=${r}, color=${color}, r=${colorMap.red}, g=${colorMap.green}, b=${colorMap.blue}, a=${colorMap.alpha}"
        }
    }

    Map<String, Integer> splitArgb(int argb) {
        return [
            'alpha':  ((argb >> 24) & 0xff),
            'red':    ((argb >> 16) & 0xff),
            'green':  ((argb >>  8) & 0xff),
            'blue':   ((argb      ) & 0xff),
        ]
    }

    int distance(int x1, int y1, int x2, int y2) {
        int a = x1 - x2;
        int b = y1 - y2;
        return Math.sqrt(a * a + b * b);
    }

    boolean touchesPlacedCircle(int x, int y, int r) {
        return placedCircles.find { FillCircle circle ->
            //return true immediately if any match
            return distance(x, y, circle.x, circle.y) < (circle.r + r + spacing)
        }
    }

    boolean isFilledWith(int x, int y, int color) {
        int pixelColorArgb = inputPixels[(canvasWidth * y) + x]
        return color == pixelColorArgb
    }

    boolean isCircleInside(int x, int y, int r, int color) {
        if (x + r >= canvasWidth ||
            y + r >= canvasHeight || 
            x - r < 0 || 
            y - r < 0) {
                // Circle is off the canvas
                return false
        }

        if (!isFilledWith(x, y, color)) return false
        //--use 4 points around circle as good enough approximation
        if (!isFilledWith(x, y - r, color)) return false
        if (!isFilledWith(x, y + r, color)) return false
        if (!isFilledWith(x + r, y, color)) return false
        if (!isFilledWith(x - r, y, color)) return false

        //--use another 4 points between the others as better approximation
        int o = r * Math.cos(Math.PI / 4);
        if (!isFilledWith(x + o, y + o, color)) return false
        if (!isFilledWith(x - o, y + o, color)) return false
        if (!isFilledWith(x - o, y - o, color)) return false
        if (!isFilledWith(x + o, y - o, color)) return false

        return true
    }

    void placeCircles() {
        placedCircles.clear()
        for (int currRadius = maxSize; currRadius >= minSize; currRadius--) {
            for (int numAtSize = 0; numAtSize < numCirclesPerSize; numAtSize++) {
                int numRetries = numRetriesPerCircle
                boolean circlePlaced = false
                while (!circlePlaced && numRetries-- > 0) {
                    int x = random.nextDouble() * canvasWidth
                    int y = random.nextDouble() * canvasHeight
                    int color = inputPixels[(canvasWidth * y) + x]
                    if (!omitColors.contains(color) && isCircleInside(x, y, currRadius, color)) {
                        if (!touchesPlacedCircle(x, y, currRadius)) {
                            circlePlaced = true
                            FillCircle placedCircle = new FillCircle(currRadius)
                            placedCircle.x = x
                            placedCircle.y = y
                            placedCircle.color = color
                            placedCircles << placedCircle

                            if (doubleOffset != null) {
                                def offsetCircle = placedCircle.clone()
                                offsetCircle.r -= doubleOffset
                                placedCircles << offsetCircle
                            }

                            print "."
                            break
                        }
                    }
                }
                if (!circlePlaced) {
                    print("-[${currRadius}]")
                }
            }
        }
        println ""
        println " - done"
    }

    void drawCircles(SVGGraphics2D output) {
        placedCircles.each { FillCircle circle ->
            output.setPaint(new Color(circle.color))
            output.fillArc(
                (int) Math.round(circle.x - circle.r), 
                (int) Math.round(circle.y - circle.r), 
                (int) Math.round(circle.r * 2), 
                (int) Math.round(circle.r * 2),
                0, 
                360)
        }
    }

    Integer call() throws Exception {
        BufferedImage im = ImageIO.read(inputPngFile)
        int transparentColor = im.getTransparency()
        canvasWidth = im.getWidth()
        canvasHeight = im.getHeight()
        // Could also let getRGB allocate this array, but doing it like this to make it more similar to Android
        inputPixels = new int[im.getWidth() * im.getHeight()];
        im.getRGB(0, 0, im.getWidth(), im.getHeight(), inputPixels, 0, im.getWidth());

        println '------- placing circles'
        placeCircles()
        placedCircles.each {
            println it
        }

        println '------- draw'
        SVGGraphics2D output = new SVGGraphics2D(canvasWidth, canvasHeight, SVGUnits.PX);
        drawCircles(output);

        println '------- output'
        String outputSvg = output.getSVGElement();
        outputSvgFile.write outputSvg
        println "${placedCircles.size()} circles written to file"
        println "omitted colors=${omitColors}"
        return 0
    }

    static void main(String[] args) {
        System.exit(new CommandLine(new png2svgcircles()).execute(args))
    }
}
