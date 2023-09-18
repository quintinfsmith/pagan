import xml.etree.ElementTree as ET
import os
tree = ET.parse('app/score.mscx')
root = tree.getroot()

LIST_ELEMENTS = [
    ( "Score", "Staff", "Measure", "Voice" ),
]

global_mapped = set()

def camel_to_snake(string):
    for i in range(26):
        string = string.replace(chr(65 + i), f" {chr(97 + i)}")
    string = string.strip()
    while "  " in string:
        string = string.replace("  ", " ")
    string = string.replace(" ", "_")
    return string.lower()

def any_to_camel(string):
    string = string.replace("_", " ")
    parts = string.split(" ")
    output = ""
    for part in parts:
        if ord(part[0]) >= 97:
            part = chr(ord(part[0]) - 32) + part[1:]
        output += part
    return output

def generate_kotlin(tag, path):
    pathstr = "/".join(path)

    strings = []
    class_name = any_to_camel(tag.tag)

    if class_name == "Text":
        class_name = "MSText"

    lists = set()
    singles = set()
    tagnames = set()
    types = {} # 0 = Int, 1 = string
    for child in tag:
        if child.tag in tagnames:
            if child.tag in singles:
                lists.add(child.tag)
                singles.remove(child.tag)
        else:
            singles.add(child.tag)
            tagnames.add(child.tag)
            if len(child) or child.attrib:
                types[child.tag] = 2
            else:
                try:
                    int(child.text)
                    types[child.tag] = 0
                except:
                    types[child.tag] = 1

    indent_a = " " * (4 * (len(path) - 1))
    indent_b = " " * (4 * len(path))
    output = f"{indent_a}@Root(strict = false, name = \"{tag.tag}\")\n"
    output += f"{indent_a}class {class_name} {{\n"

    new_tag_map = {}
    for child in tag:
        if child.tag not in new_tag_map:
            new_tag_map[child.tag] = ET.Element(child.tag)

        working_child = new_tag_map[child.tag]
        this_done = set()
        for grandchild in child:
            if grandchild.tag not in global_mapped:
                working_child.append(grandchild)
            key = path.copy()
            this_done.add(grandchild.ta)
        global_mapped = global_mapped.union(this_done)
        new_tag_map[child.tag] = working_child

        for k, v in child.attrib.items():
            working_child.attrib[k] = v

    for _, child in new_tag_map.items():
        new_path = path.copy()
        new_path.append(any_to_camel(child.tag))
        output += generate_kotlin(child, new_path) + "\n"



    for k, v in tag.attrib.items():
        attr_name = k.lower().replace(" ", "_")
        output += f"{indent_b}@field:Attribute(name = \"{k}\", required = false)\n"
        try:
            int(v)
            output += f"{indent_b}var {attr_name}: Int? = null"
        except:
            output += f"{indent_b}var {attr_name}: String? = null"
        output += "\n\n"
    if tuple(path[1:]) not in LIST_ELEMENTS:
        output += f"{indent_b}//{path}\n"
        for name in singles:
            elm_name = camel_to_snake(name)
            child_class_name = any_to_camel(name)
            if child_class_name == "Text":
                child_class_name = "MSText"
                elm_name = "mstext"
            output += f"{indent_b}@field:Element(name = \"{name}\", required = false)\n"
            output += f"{indent_b}lateinit var {elm_name}: {child_class_name}\n\n"

        for name in lists:
            elm_name = camel_to_snake(name)
            child_class_name = any_to_camel(name)
            if child_class_name == "Text":
                child_class_name = "MSText"
                elm_name = "mstext"
            output += f"{indent_b}@field:ElementList(entry = \"{name}\", required = false, inline=true)\n"
            output += f"{indent_b}lateinit var {elm_name}_list: List<{child_class_name}>\n\n"
    else:
        indent_c = " " * (4 * (len(path) + 1))
        output += f"{indent_b}@field:ElementListUnion(\n"
        lines = []
        for name in lists:
            child_class_name = any_to_camel(name)
            lines.append(f"{indent_c}ElementList(entry = \"{name}\", inline = true, type = {child_class_name}::class, required = false)")
        for name in singles:
            child_class_name = any_to_camel(name)
            lines.append(f"{indent_c}ElementList(entry = \"{name}\", inline = true, type = {child_class_name}::class, required = false)")
        output += ",\n".join(lines) + f"\n{indent_b})\n"
        output += f"{indent_b}lateinit var elements: List<Any>\n"

    if len(tag) == 0:
        output += f"{indent_b}@field:Text(required = false)\n"
        output += f"{indent_b}var text: String = \"\"\n"

    output += f"{indent_a}}}"

    return output

print("package com.qfs.pagan\n")
print("import org.simpleframework.xml.Attribute")
print("import org.simpleframework.xml.Text")
print("import org.simpleframework.xml.Element")
print("import org.simpleframework.xml.ElementList")
print("import org.simpleframework.xml.ElementListUnion")
print("import org.simpleframework.xml.Root")
print(generate_kotlin(root, ["musescore"]))
