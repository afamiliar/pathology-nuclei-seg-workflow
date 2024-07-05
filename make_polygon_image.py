# Takes the QuPath output GeoJSON file & create a labelled annotation image
#       set up to operate on 1 QuPath "Annotation" object containing N nuclei "detection" objects

from PIL import Image
from PIL import ImageDraw
import random
import geojson
from glob import glob

def format_coordinates(poly_coords):
    clean_coords = []
    ind=0
    for coord in poly_coords:
        clean_coords.append(tuple(coord))
        ind+=1
    return clean_coords

# ================================================================
out_path = 'export'
geojson_files = glob(f'./{out_path}/*.geojson')

for gj_file in geojson_files:
    print(f'Processing GeoJSON file: {gj_file}')
    
    with open(gj_file) as f:
        gj = geojson.load(f)

    img_dims = gj["features"][0]["geometry"]["coordinates"][0][2]
    print(f'               image dimensions: {img_dims}')

    # img = Image.open('./Tiles/53.png').convert('RGBA')
    img = Image.new('RGB', (img_dims[0], img_dims[0])) # create a black image

    img2 = img.copy()
    draw = ImageDraw.Draw(img2)

    used_colors = [(0,0,0), (255,255,255)]
    for json_obj in gj["features"]:
        if json_obj["properties"]["objectType"] == "detection": # if it's a QuPath detection
            # get the polygon coordinates
            these_coords = json_obj["geometry"]["coordinates"][0]
            # transform into the coord format needed for PIL
            formatted_coords = format_coordinates(these_coords)
            # make a unique RGB value for this polygon
            unique_color = 0
            while unique_color == 0:
                color_code = (random.randrange(0, 255), random.randrange(0, 255), random.randrange(0, 255))
                if color_code not in used_colors:
                    used_colors.append(color_code)
                    unique_color = 1
            # draw the polygon
            draw.polygon(formatted_coords, fill = color_code)

    img3 = Image.blend(img, img2, 1)
    # img3 = Image.blend(img, img2, 0.5) # transparent overlay

    out_fn = gj_file.split('.geojson')[0]
    out_fn = out_fn.split('_')[-1]
    img3.save(f'./{out_path}/{out_fn}_labeled_mask.png')
