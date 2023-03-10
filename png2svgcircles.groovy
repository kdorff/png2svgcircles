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
 *   just make circles from minRadius to maxRadius.
 */

@Command(name = 'png2svgcircles.groovy', mixinStandardHelpOptions = true, version = '0.1',
        description = 'Draws an SVG using random circles from a simple PNG')
class png2svgcircles implements Callable<Integer> {
    @Option(names = ["-i", "--input"], required = true, description = "The input PNG file")
    File inputPngFile

    @Option(names = ["-o", "--output"], required = true, description = "The output SVG file")
    File outputSvgFile

    @Option(names = ["-s", "--spacing"], description = "Minimum space between circles - default is 10")
    int spacing = 5

    @Option(names = ["-n", "--num-circles"], description = "Number of circles to try to place - default is 2000")
    int numCircles = 2000

    @Option(names = ["-r", "--retries"], description = "Number of retries for placing each circle - default is 1000")
    int numRetriesPerCircle = 1000

    @Option(names = ["-m", "--min-radius"], description = "The minimum circle radius - default is 10")
    int minRadius = 10

    @Option(names = ["-x", "--max-radius"], description = "The maximum circle radius - default is 50")
    int maxRadius = 50

    @Option(names = ["-d", "--double-circles-offset"], description = "Double the placed circles with each doubled circle having the radius reduced by this much")
    Double doubleOffset = null

    @Option(names = ["-c", "--color-skip"], description = "Colors (argb values) to be omitted. Can be included multiple times to omit multiple colors")
    List<Integer> omitColors = []

    @Option(names = ["-t", "--color-tolerance"], description = "Color tolerance for color matching, in percentage. Range is 0.0 (any color will match) to 1.0 (exact color match required). The alpha channel is ignored. Default is 1.0")
    Double tolerance = 1.0d

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


    double colorsPercentageDifference(int argb1, int argb2) {
        Map color1Map = splitArgb(argb1)
        Map color2Map = splitArgb(argb2)

        // TODO: Should we take the alpha channel into account? We don't.
        double distance = Math.sqrt(
            (color2Map.red - color1Map.red)^2 +
            (color2Map.green - color1Map.green)^2 +
            (color2Map.blue - color1Map.blue)^2)
        double percentage = distance / Math.sqrt((255)^2+(255)^2+(255)^2)
        return percentage
    }

    boolean isFilledWith(int x, int y, int color) {
        int pixelColorArgb = inputPixels[(canvasWidth * y) + x]
        if (tolerance == 1.0d) {
            // 100% match required. Only match the same color.
            return color == pixelColorArgb
        }
        else if (tolerance == 0.0d) {
            // Always match. Weird, but OK.
            return true
        }
        else {
            Double perDiff = colorsPercentageDifference(color, pixelColorArgb)
            return perDiff < tolerance
        }
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

    public int getRandomInRange(int minInclusive, int maxExclusive) {
        int maxInclusive = maxExclusive + 1;
        return (int) ((Math.random() * (maxInclusive - minInclusive)) + minInclusive);
    }

    void placeCircles() {
        placedCircles.clear()
        for (int numTried = 0; numTried < numCircles; numTried++) {
            int numRetries = numRetriesPerCircle
            boolean circlePlaced = false
            int currRadius = getRandomInRange(minRadius, maxRadius)
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

                        print "[$currRadius]"
                    }
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
