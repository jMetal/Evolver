# Configuration file for the Sphinx documentation builder.

# -- Project information -----------------------------------------------------
project = 'Evolver'
copyright = '2025, Antonio J. Nebro'
author = 'Antonio J. Nebro'

# The full version, including alpha/beta/rc tags
release = '2.5-SNAPSHOT'

# -- General configuration ---------------------------------------------------

# Add any Sphinx extension module names here, as strings.
extensions = [
    'sphinx.ext.autodoc',
    'sphinx.ext.napoleon',
    'sphinx.ext.viewcode',
    'sphinx.ext.githubpages',
    'sphinx.ext.graphviz',
    'sphinx_copybutton',
    'sphinx_design',
]

# Add any paths that contain templates here, relative to this directory.
templates_path = ['_templates']

# List of patterns, relative to source directory, that match files and
# directories to ignore when looking for source files.
exclude_patterns = ['_build', 'Thumbs.db', '.DS_Store']

# -- Options for HTML output -------------------------------------------------

# The theme to use for HTML and HTML Help pages.
html_theme = 'pydata_sphinx_theme'
html_theme_options = {
    # The logo of each mode: the dark one for the dark theme.
    'logo': {
        'image_light': 'figures/logo/evolver-logo.svg',
        'image_dark': 'figures/logo/evolver-logo-dark.svg',
    },
    'github_url': 'https://github.com/jMetal/Evolver',
    'show_toc_level': 2,
    # Only the table of contents of the page: not the "Show source" block.
    'secondary_sidebar_items': ['page-toc'],
    # The menu of the site is in the left sidebar, not in the header.
    'navbar_center': [],
}
# The whole table of contents of the site, with its sections, in the left sidebar of every page.
html_sidebars = {'**': ['sidebar-global-nav']}
html_show_sourcelink = False

# Add any paths that contain custom static files (such as style sheets) here,
# relative to this directory. They are copied after the builtin static files,
# so a file named "default.css" will overwrite the builtin "default.css".
html_static_path = ['_static']
html_css_files = ['custom.css']
html_favicon = 'figures/logo/evolver-icon-tile.svg'

# -- Extension configuration -------------------------------------------------

# Auto-documentation settings
autodoc_default_options = {
    'members': True,
    'member-order': 'bysource',
    'special-members': '__init__',
    'undoc-members': True,
    'exclude-members': '__weakref__'
}

# Napoleon settings
napoleon_google_docstring = True

# Graphviz configuration
napoleon_numpy_docstring = True
napoleon_include_init_with_doc = False
napoleon_include_private_with_doc = False
napoleon_include_special_with_doc = True
napoleon_use_admonition_for_examples = False
napoleon_use_admonition_for_notes = False
napoleon_use_admonition_for_references = False
napoleon_use_ivar = False
napoleon_use_param = True
napoleon_use_rtype = True
napoleon_preprocess_types = False
napoleon_type_aliases = None
napoleon_attr_annotations = True

extensions = [
    'sphinx.ext.autodoc',
    'sphinx.ext.napoleon',
    'sphinx.ext.viewcode',
    'sphinx.ext.autosummary',
]
