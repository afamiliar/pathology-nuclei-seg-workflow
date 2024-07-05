// produces original ROI image + grayscale of cells
/**
 * This script provides a general template for nucleus detection using StarDist in QuPath.
 * This example assumes you have an RGB color image, e.g. a brightfield H&E slide.
 * 
 * If you use this in published work, please remember to cite *both*:
 *  - the original StarDist paper (https://doi.org/10.48550/arXiv.1806.03535)
 *  - the original QuPath paper (https://doi.org/10.1038/s41598-017-17204-5)
 *  
 * There are lots of options to customize the detection - this script shows some 
 * of the main ones. Check out other scripts and the QuPath docs for more info.
 */

import qupath.ext.stardist.StarDist2D
import qupath.lib.scripting.QP
import qupath.lib.images.servers.LabeledImageServer
import org.apache.commons.io.FileUtils;
 
// IMPORTANT! Replace this with the path to your StarDist model
// that takes 3 channel RGB as input (e.g. he_heavy_augment.pb)
// You can find some at https://github.com/qupath/models
// (Check credit & reuse info before downloading)
def modelPath = '/Users/familiara/Downloads/he_heavy_augment.pb'
selectAnnotations();
clearSelectedObjects(false);
createFullImageAnnotation(true)

//Set threshold for nuclei detection
def threshold = 0.13

// Customize how the StarDist detection should be applied
// Here some reasonable default options are specified
def stardist = StarDist2D
    .builder(modelPath)
    .normalizePercentiles(1, 99) // Percentile normalization
    .threshold(threshold)              // Probability (detection) threshold
    .pixelSize(0.5)              // Resolution for detection
    .measureShape()              // Add shape measurements
    .includeProbability(true)
    .measureIntensity()          // Add nucleus measurements
    .build()
	 
// Define which objects will be used as the 'parents' for detection
// Use QP.getAnnotationObjects() if you want to use all annotations, rather than selected objects

def pathObjects = QP.getAnnotationObjects()
 
// Run detection for the selected objects
def imageData = QP.getCurrentImageData()
if (pathObjects.isEmpty()) {
    QP.getLogger().error("No parent objects are selected!")
    return
}
stardist.detectObjects(imageData, pathObjects)
stardist.close() // This can help clean up & regain memory
println('Done!')

imageData = getCurrentImageData()

// Define output path (relative to project)
def name = GeneralTools.getNameWithoutExtension(imageData.getServer().getMetadata().getName())

print(name)
def pathOutput = buildFilePath(PROJECT_BASE_DIR, 'export')
mkdirs(pathOutput)
def File dir = new File(pathOutput)
for (File f : dir.listFiles()) {
    if (f.getName().startsWith(name)) {
        f.delete();
    }
}

// Convert to downsample
double downsample = 1

// Create an ImageServer where the pixels are derived from annotations
def labelServer = new LabeledImageServer.Builder(imageData)
    .backgroundLabel(0, ColorTools.BLACK) // Specify background label (usually 0 or 255)
    //.useCellNuclei()
    .useDetections()
    .addUnclassifiedLabel(1, ColorTools.WHITE)
    //.lineThickness(0.0001)
    //.addLabel('Nuclei', 1, ColorTools.WHITE)      // Choose output labels (the order matters!)
    .downsample(downsample)    // Choose server resolution; this should match the resolution at which tiles are exported
    .multichannelOutput(false)  // If true, each label is a different channel (required for multiclass probability)
    .build()

// Export each region
//def regions_of_int= getAnnotationObjects().findAll { it.getPathClass()==null }
def regions_of_int= getAnnotationObjects()

int i = 0
for (annotation in regions_of_int) {
    def region = RegionRequest.createInstance(
        labelServer.getPath(), downsample, annotation.getROI())
    i++
    def outputPath = buildFilePath(pathOutput, name + '_mask_' + Math.round(threshold*100) + '.tif')
    //automatically fills in the file name with the set threshold
    writeImageRegion(labelServer, region, outputPath)
    def region2 = RegionRequest.createInstance(
        imageData.getServer().getPath(), downsample, annotation.getROI())
    def outputPath2 = buildFilePath(pathOutput, 'Original_' + name + '.png')
    writeImageRegion(imageData.getServer(), region2, outputPath2)
}

// Export GeoJSON file
def path = buildFilePath(pathOutput, 'detections_' + name + '.geojson')
// 'FEATURE_COLLECTION' is standard GeoJSON format for multiple objects
exportAllObjectsToGeoJson(path, "EXCLUDE_MEASUREMENTS", "PRETTY_JSON", "FEATURE_COLLECTION")

print 'Done'