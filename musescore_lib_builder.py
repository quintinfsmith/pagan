import xml.etree.ElementTree as ET
tree = ET.parse('score.mscx')
root = tree.getroot()


print(root.tag, root.attrib, root.text)
for child in root:
    print("\t", child.tag, child.attrib, child.text, f"-{len(child)}-")


def generate_kotlin(tag):
    class_name = tag.tag.title().replace(" ", "")
    output = f"class {class_name} {{\n"
    for k, v in tag.attrib.items():
        attr_name = k.lower().replace(" ", "_")
        output += f"    @field:Attribute(name = \"{k}\", required = true)\n"
        try:
            int(v)
            output += f"    var {attr_name}: Int"
        except:
            output += f"    var {attr_name}: String"
        output += "\n\n"

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
            if len(child):
                types[child.tag] = 2
            else:
                try:
                    int(child.text)
                    types[child.tag] = 0
                except:
                    types[child.tag] = 1

    for name in singles:
        elm_name = name.lower().replace(" ", "_")
        output += f"    @field:Element(name = \"{name}\", required = true)\n"
        output += f"    lateinit var {elm_name}: {name}\n\n"

    for name in lists:
        elm_name = name.lower().replace(" ", "_")
        output += f"    @field:Element(name = \"{name}\", required = true)\n"
        output += f"    lateinit var {elm_name}_list: List<{name}>\n\n"


    output += f"\n}}"
    return output

print(generate_kotlin(root))
for child in root:
    print(generate_kotlin(child))
