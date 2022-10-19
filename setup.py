import setuptools
from src import __version__, __author__, __email__, __url__, __license__

with open("README.md", "r") as fh:
    long_description = fh.read()

setuptools.setup(
    name="radixulous",
    version=__version__,
    description="Radix-based midi editor",
    author=__author__,
    author_email=__email__,
    install_requires=['pyinotify', 'wrecked', 'apres'],
    long_description=long_description,
    long_description_content_type="text/markdown",
    license=__license__,
    keywords=[],
    python_requires="~=3.7",
    py_modules=["radixulous"],
    entry_points={ "console_scripts": ["radixulous = radixulous:main"] },
    url=__url__,
    packages=setuptools.find_packages(),
    classifiers=[
        "Programming Language :: Python :: 3",
        "License :: OSI Approved :: GNU General Public License v2 (GPLv2)",
        "Operating System :: POSIX :: Linux",
    ]
)
