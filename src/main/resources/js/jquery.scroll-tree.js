(function($) {

    'use strict';

    // http://learn.jquery.com/plugins/basic-plugin-creation/
    // http://learn.jquery.com/plugins/advanced-plugin-concepts/
    $.fn.scrollTree = function (options) {

        var DEFAULT_OPTIONS = {
            'contextPath': '/',
            'css': {
                'ancestor': 'active',
                'current': 'active',
                "leaf": 'leaf',
                'loading': 'sp-loading',
                'collapsed': 'sp-collapsed',
                'expanded': 'sp-expanded',
                'error': 'sp-error'
            },
            'renderChildrenUl': function () {
                return '<ul class="nav"></ul>';
            },
            'renderChildLi': function (child, opts) {
                return '<li class="' + opts.css[child.type] + '"><span class="sp-toggle"></span><a href="' + child.link + '">' + child.title + '</a></li>'
            }
        };

        var viewportId = $(this).data('viewportId');
        var rootLink = $(this).data('root');
        var currentLink = $(this).data('current');

      	// PTF-specific variables
        var confluenceSpaceRootId = $(this).data('confluenceid');
      	var currentTitle = $(this).data('currenttitle');

        var opts = $.extend(true, DEFAULT_OPTIONS, options);

        return this.each(function () {
            var $rootUl = $(this);

            loadChildren($rootUl, rootLink, currentTitle);
            setupEventHandling($rootUl);

            return this;
        });
 
        function loadChildren($ul, parentTitle, currentTitle) {
            var $parentLi = $ul.closest('li');
            if ($parentLi) {
                $parentLi.removeClass(opts.css.collapsed)
                    .addClass(opts.css.loading);
            }
 			var label = sessionStorage.getItem('label');
          	// CUSTOM ENDPOINT 1
          	// Called on page load
            $.get(opts.contextPath + '/rest/treefilter/1.0/getchildrenrecursive', {
                'spaceId': confluenceSpaceRootId,
                'parent': parentTitle || $parentLi.find('> a').text(),
                'current': currentTitle || '',
                'label': 'mvc' // Will use the cookie / page context, hardcoded for testing purposes
            })
                .success(function success(children) {
                    insertChildren($ul, children);

                    $parentLi.removeClass(opts.css.loading)
                        .addClass(opts.css.expanded);
                })
                .error(function error(jqXHR, textStatus, errorThrown) {
                    $parentLi.removeClass(opts.css.loading)
                        .addClass(opts.css.error);
                })
            ;
        }

        function loadDirectDescendants($ul, parentTitle, currentTitle) {
            var $parentLi = $ul.closest('li');
            if ($parentLi) {
                $parentLi.removeClass(opts.css.collapsed)
                    .addClass(opts.css.loading);
            }
 			var label = sessionStorage.getItem('label');
          	// CUSTOM ENDPOINT 2 - called when opening subtrees outside the original page load request
            // uses DOM to persist already loaded page links to avoid repeated use of recursion
            $.get(opts.contextPath + '/rest/treefilter/1.0/getchildren', {
                'spaceId': confluenceSpaceRootId,
                'parent': parentTitle || $parentLi.find('> a').text(),
              	'parentLink':  $parentLi.find('> a').attr('href'),
                'label': 'mvc' // Will use the cookie / page context, hardcoded for testing purposes
            })
                .success(function success(children) {
                    insertChildren($ul, children);

                    $parentLi.removeClass(opts.css.loading)
                        .addClass(opts.css.expanded);
                })
                .error(function error(jqXHR, textStatus, errorThrown) {
                    $parentLi.removeClass(opts.css.loading)
                        .addClass(opts.css.error);
                })
            ;
        }

        function insertChildren($ul, children) {
            $ul.html('');
            $.each(children, function (idx, child) {
                var $childLi = $(opts.renderChildLi(child, opts)).appendTo($ul);

                if (child.children) {
                    if (child.children.length) {
                        $childLi.addClass(opts.css.expanded);
                        var $childrenEl = $(opts.renderChildrenUl()).appendTo($childLi);
                        insertChildren($childrenEl, child.children);

                    } else {
                        $childLi.addClass(opts.css.collapsed);
                    }
                } else {
                    $childLi.addClass(opts.css.leaf);
                }
            });
        }

        function setupEventHandling($rootUl) {
            $rootUl.on('click', '.sp-toggle', function () {
                var $li = $(this).parent('li');
                if ($li.is('.' + opts.css.collapsed)) {
                    openNode($li);

                } else if ($li.is('.' + opts.css.expanded)) {
                    closeNode($li);

                } else {
                    // we don't have children -> no-op
                }
            });
        }

        function openNode($li) {
            if ($li.has('ul').length) {
                // children have been loaded, just toggle classes
                $li.removeClass(opts.css.collapsed)
                    .addClass(opts.css.expanded);
            } else {
                // children have to be loaded
                var $childrenEl = $(opts.renderChildrenUl()).appendTo($li);
                loadDirectDescendants($childrenEl);
            }
        }

        function closeNode($li) {
            $li
                .removeClass(opts.css.expanded)
                .addClass(opts.css.collapsed);
        }
    };

})($);