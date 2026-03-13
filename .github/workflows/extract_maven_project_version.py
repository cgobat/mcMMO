#!/usr/bin/env python

from pathlib import Path
from xml.etree import ElementTree as ET

WORKFLOWS_DIR = Path(__file__).resolve().parent
REPO_DIR = WORKFLOWS_DIR.parent.parent
POM_XML_PATH = REPO_DIR/"pom.xml"

def read_version_from_pom_xml(pom_file_path: Path):
    nsmap = {}
    for _, e in ET.iterparse(pom_file_path, events=("start-ns",)):
        prefix, uri = e
        nsmap[prefix] = uri
    
    doc = ET.parse(pom_file_path)
    project = doc.getroot()
    version = project.find("version", namespaces=nsmap)
    return version.text.strip()

if __name__ == "__main__":
    version = read_version_from_pom_xml(POM_XML_PATH)
    print(version)
