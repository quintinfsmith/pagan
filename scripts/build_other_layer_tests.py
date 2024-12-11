#!/usr/bin/python
"""
    Generates UnitTests based on the existing OpusLayer<layer>UnitTest.kt files using higher-level layers
    to make sure the functionality isn't broken by any layer
"""
import os

file_name = __file__[__file__.rfind("/") + 1:]
disclaimer_text = f"""/*
* DO NOT MODIFY THE CONTENTS OF THIS FILE. IT WAS GENERATED IN /scripts/{file_name}
*/
"""

# Classes In heirarchichal order
test_classes = [
    "OpusLayerBase",
    "OpusLayerHistory",
    #"OpusLayerCursor"
]
working_directory = __file__[0:__file__.rfind("/")]
TEST_FILE_DIR = f"{working_directory}/../app/src/test/java/com/qfs/pagan"

for (i, test_class) in enumerate(test_classes):
    with open(f"{TEST_FILE_DIR}/{test_class}UnitTest.kt", "r", encoding="utf8") as fp:
        file_content = fp.read()
    for (j, retest_class) in enumerate(test_classes[i + 1:]):
        new_content = file_content.replace(f"com.qfs.pagan.opusmanager.{test_class} as OpusManager", f"com.qfs.pagan.opusmanager.{retest_class} as OpusManager")
        new_content = new_content.replace(f"class {test_class}UnitTest", f"class {test_class}UnitReTestAs{retest_class}")
        new_file_name = f"{test_class}UnitReTestAs{retest_class}.kt"
        with open(f"{TEST_FILE_DIR}/{new_file_name}", "w", encoding="utf8") as fp:
            fp.write(disclaimer_text + new_content)

