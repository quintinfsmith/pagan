import xml.etree.ElementTree as ET
import os
tree = ET.parse('app/score.mscx')
root = tree.getroot()

MIXED_LIST_ELEMENTS = [
    ( "MuseScore", "Score", "Staff", "Measure", "Voice" ),
]

#KNOWN_LIST_ELEMENTS = [
#    ( "Score", "Staff", "Measure", "Voice", "Chord", "Note" ),
#    ( "Score", "Staff", "Measure", "Voice", "Chord", "Note", "Spanner" )
#]

children_accounted_for = {}

def count_children(tag, path, count_map):
    output = set()
    class_name = any_to_camel(tag.tag)
    path.append(class_name)

    current_counts = {}
    for child in tag:
        k = any_to_camel(child.tag)
        current_counts[k] = current_counts.get(k, 0) + 1

    for (k, v) in current_counts.items():
        current_key = path.copy()
        current_key.append(k)
        current_key = tuple(current_key)
        if current_key not in count_map:
            count_map[current_key] = 0
        count_map[current_key] = max(count_map[current_key], v)

    for child in tag:
        count_children(child, path.copy(), count_map)



def camel_to_snake(string):
    for i in range(26):
        string = string.replace(chr(65 + i), f" {chr(97 + i)}")
    string = string.strip()
    while "  " in string:
        string = string.replace("  ", " ")
    string = string.replace("-", "_")
    string = string.replace(" ", "_")
    return string.lower()

def any_to_camel(string):
    string = string.replace("_", " ")
    string = string.replace("-", " ")
    parts = string.split(" ")
    output = ""
    for part in parts:
        if ord(part[0]) >= 97:
            part = chr(ord(part[0]) - 32) + part[1:]
        output += part
    return output

def generate_kotlin(tag, path, child_counts):
    pathstr = "/".join(path)

    strings = []
    class_name = any_to_camel(tag.tag)
    path.append(class_name)

    if class_name == "Text":
        class_name = "MSText"

    indent_a = " " * (4 * (len(path) - 1))
    indent_b = " " * (4 * len(path))
    output = f"{indent_a}@Root(strict = false, name = \"{tag.tag}\")\n"
    output += f"{indent_a}class {class_name} {{\n"
    output += f"{indent_b}// {'|'.join(path)}\n"

    new_tag_map = {}
    for child in tag:
        if child.tag not in new_tag_map:
            new_tag_map[child.tag] = ET.Element(child.tag)

        working_child = new_tag_map[child.tag]

        key = path.copy()
        key.append(any_to_camel(child.tag))
        key = tuple(key)
        if key not in children_accounted_for:
            children_accounted_for[key] = set()

        for grandchild in child:
            working_child.append(grandchild)

        new_tag_map[child.tag] = working_child

        for k, v in child.attrib.items():
            working_child.attrib[k] = v

    generated_tags = set()
    tag_names = set()
    for _, child in new_tag_map.items():
        if child.tag.lower() in generated_tags:
            continue
        output += generate_kotlin(child, path.copy(), child_counts) + "\n"
        generated_tags.add(child.tag.lower())
        tag_names.add(child.tag)

    for k, v in tag.attrib.items():
        attr_name = k.lower().replace(" ", "_")
        output += f"{indent_b}@field:Attribute(name = \"{k}\", required = false)\n"
        try:
            int(v)
            output += f"{indent_b}var {attr_name}: Int? = null"
        except:
            output += f"{indent_b}var {attr_name}: String? = null"
        output += "\n\n"

    if tuple(path) not in MIXED_LIST_ELEMENTS:
        for name in tag_names:
            elm_name = camel_to_snake(name)
            child_class_name = any_to_camel(name)
            if child_class_name == "Text":
                child_class_name = "MSText"
                elm_name = "mstext"

            key = path.copy()
            key.append(child_class_name)
            key = tuple(key)
            if child_counts.get(key, 0) == 1:
                output += f"{indent_b}@field:Element(name = \"{name}\", required = false)\n"
                output += f"{indent_b}lateinit var {elm_name}: {child_class_name}\n\n"
            else:
                output += f"{indent_b}@field:ElementList(entry = \"{name}\", required = false, inline=true)\n"
                output += f"{indent_b}lateinit var {elm_name}_list: List<{child_class_name}>\n\n"
    else:
        indent_c = " " * (4 * (len(path) + 1))
        output += f"{indent_b}@field:ElementListUnion(\n"
        lines = []
        for name in tag_names:
            elm_name = camel_to_snake(name)
            child_class_name = any_to_camel(name)
            lines.append(f"{indent_c}ElementList(entry = \"{name}\", inline = true, type = {child_class_name}::class, required = false)")

        output += ",\n".join(lines) + f"\n{indent_b})\n"
        output += f"{indent_b}lateinit var elements: List<Any>\n"

    if len(tag) == 0:
        output += f"{indent_b}@field:Text(required = false)\n"
        output += f"{indent_b}var text: String = \"\"\n"

    output += f"{indent_a}}}"

    return output
child_counts = {}
count_children(root, [], child_counts)
print("package com.qfs.pagan")
print("// DO NOT EDIT. This file was generated with musescore_lib_builder.py")
print("import org.simpleframework.xml.Attribute")
print("import org.simpleframework.xml.Text")
print("import org.simpleframework.xml.Element")
print("import org.simpleframework.xml.ElementList")
print("import org.simpleframework.xml.ElementListUnion")
print("import org.simpleframework.xml.Root")
print(generate_kotlin(root, [], child_counts))
