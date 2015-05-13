'use strict';

/* Directives */
var appDirectives = angular.module('appDirectives', []);

appDirectives.directive('treeview', [ function () {
	return {
		restrict: 'A',
		scope: {
	        options: '=treeview'
	    },
		link: function (scope, element, attrs) {
			element.tree();
		}
	};
}])
