'use strict';

/* Directives */
var appDirectives = angular.module('appDirectives', []);

appDirectives.directive('tagSelect', [ function () {
	return {
		restrict: 'A',
		scope: {
	        options: '=tagSelect'
	    },
		link: function (scope, element, attrs) {
			var data = scope.options['data'];
			var type = scope.options['type'];
			var tags = new Bloodhound({
			  datumTokenizer: Bloodhound.tokenizers.obj.whitespace('name'),
			  queryTokenizer: Bloodhound.tokenizers.whitespace,
			  remote: 'http://backend.com/api/tags?query=%QUERY'
			});

			tags.initialize();

			element.typeahead(null, {
			  name: 'tags',
			  displayKey: 'name',
			  source: tags.ttAdapter()
			}).bind("typeahead:selected", function(obj, datum, name) {
			  safeApply(scope, function(){ data[type].tags.push({title: datum.name, id: datum.id}) });
			  element.typeahead('val', '');
			})
			.bind("typeahead:autocompleted", function(obj, datum, name) {
				safeApply(scope, function(){ data[type].tags.push({title: datum.name, id: datum.id}) });
			  element.typeahead('val', '');
			}).bind("typeahead:opened", function(obj, datum, name) {
			}).bind("typeahead:newContent", function(obj, query) {
				safeApply(scope, function(){ data[type].tags.push({title: query}) });
			  element.typeahead('val', '');
			});
		}
	};
}]);

appDirectives.directive('autoResize', [ function () {
	return {
		restrict: 'A',
		scope: {
	        options: '=autoResize'
	    },
		link: function (scope, element, attrs) {
			element.autosize();
		}
	};
}])

appDirectives.directive('tagDeselect', [ function () {
	return {
		restrict: 'A',
		scope: {
	        options: '=tagDeselect'
	    },
		link: function (scope, element, attrs) {
			var data = scope.options['data'];
			var type = scope.options['type'];
			var id = scope.options['id'];

			element.on('click', function(){
				safeApply(scope, function(){ 
					angular.forEach(data[type].tags, function(v, i) {
					  if(v.id == id) {
					    data[type].tags.splice(i, 1);
					  }
					})
				});
			})
		}
	};
}])

appDirectives.directive('userInfo', [ function () {
	return {
		restrict: 'A',
		scope: {
	        options: '=userInfo'
	  },
		link: function ($scope, element, attrs) {
			var tags = $scope.options.tags;
			var user = $scope.options.user;
			var score = user.score;

			if(!$.isEmptyObject(score)) {
				var title = '<b> Score </b>: ' + '<b>' + user.score['////USER_SCORE////'] + '</b>';
				var content = '<ul class="score">';

				console.log("title is: ", title);
				angular.forEach(tags, function(v, k) {
					if(score[v.title] != undefined) {
						var line = "<li>";
						line += '<b>' + v.title + '</b>: <b>' + score[v.title] + '</b>';
						line += "</li>";
						content = content + line;
					}

				})

				content = content + "</ul>"

				var defaultOptions = {
					trigger: 'hover',
					placement: 'auto',
					delay: { 
						show: 100, hide: 500 
					},
					html: true,
					title: title,
					content: content
				};

				element.popover(defaultOptions);
			}
		}
	};
}])

appDirectives.directive('timeago', ['$compile', function ($compile) {
	return {
		restrict: 'A',
		scope: {
	        options: '=timeago'
	  },
		link: function ($scope, element, attrs) {
			var time = $scope.options;
			var $period    =   '';
			var $secsago   =   Math.round(((new Date()).getTime() - time)/1000);

			if ($secsago < 60){
			   $period = $secsago + ' giây';
			}
			else if ($secsago < 3600) {
			   $period    =   Math.round($secsago/60);
			   $period    =   $period + ' phút';
			}
			else if ($secsago < 86400) {
			   $period    =   Math.round($secsago/3600);
			   $period    =   $period + ' giờ';
			}
			else if ($secsago < 604800) {
			   $period    =   Math.round($secsago/86400);
			   $period    =   $period + ' ngày';
			}
			else if ($secsago < 2419200) {
			   $period    =   Math.round($secsago/604800);
			   $period    =   $period + ' tuần';
			}
			else if ($secsago < 29030400) {
			   $period    =   Math.round($secsago/2419200);
			   $period    =   $period + ' tháng';
			}
			else {
			   $period    =   Math.round($secsago/29030400);
			   $period    =   $period + ' năm';
			}
			element.html($period + " trước");
		}
	};
}])

appDirectives.directive('avatarUpload', ['$compile', function ($compile) {
	return {
		restrict: 'A',
		link: function (scope, element, attrs) {
			var file, imgContent;
			var render = new FileReader();

			var input = $('<input type="file" style="display:none;"/>')
			element.append(file);

			element.click(function(){
				input.trigger('click');
			});

			input.change(function(e) {
				var files = e.target.files;
				if(files.length < 1) return;

				file = files[0];
				var imgContent = render.readAsDataURL(file);
				$('#cropableAvatar').attr('src', imgContent);
				$('#uploadAvatar').modal('show');
			});

			render.onload = function(e) {
				var bin, img;
				bin = this.result;
				img = new Image();
				img.src = bin;

				$('#cropableAvatar').attr('src', bin);
				$('#uploadAvatar').modal('show');

				var xsize = 200;
				var ysize = 200;
				$('#cropableAvatar').Jcrop({
				  onChange: updatePreview,
				  onSelect: updatePreview,
				  aspectRatio: xsize / ysize
				},function(){
				  // Use the API to get the real image size
				  var bounds = this.getBounds();
				  boundx = bounds[0];
				  boundy = bounds[1];
				  // Store the API in the jcrop_api variable
				  jcrop_api = this;

				  // Move the preview into the jcrop container for css positioning
				  $preview.appendTo(jcrop_api.ui.holder);
				});

				function updatePreview(c)
				{
				  if (parseInt(c.w) > 0)
				  {
				    var rx = xsize / c.w;
				    var ry = ysize / c.h;

				    $pimg.css({
				      width: Math.round(rx * boundx) + 'px',
				      height: Math.round(ry * boundy) + 'px',
				      marginLeft: '-' + Math.round(rx * c.x) + 'px',
				      marginTop: '-' + Math.round(ry * c.y) + 'px'
				    });
				  }
				};
			}
		}
	};
}])

appDirectives.directive('tooltip', function(){
    return {
        restrict: 'A',
        link: function(scope, element, attrs){
            $(element).hover(function(){
                // on mouseenter
                $(element).tooltip('show');
            }, function(){
                // on mouseleave
                $(element).tooltip('hide');
            });
        }
    };
});

appDirectives.directive('fbLike', ['$window', '$rootScope', function ($window, $rootScope) {
        return {
            restrict: 'A',
            scope: {
                fbLike: '=?'
            },
            link: function (scope, element, attrs) {
            	console.log("alias is: ", scope.fbLike);
                if (!$window.FB) {
                    // Load Facebook SDK if not already loaded
                    $.getScript('//connect.facebook.net/en_US/sdk.js', function () {
                        $window.FB.init({
                            appId: $rootScope.facebookAppId,
                            xfbml: true,
                            version: 'v2.0'
                        });
                        renderLikeButton();
                    });
                } else {
                    renderLikeButton();
                }

                function renderLikeButton() {
                    if (!!attrs.fbLike && !scope.fbLike) {
                        // wait for data if it hasn't loaded yet
                        scope.$watch('fbLike', function () {
                            renderLikeButton();
                        });
                        return;
                    } else {
                        element.html('<div class="fb-like"' + (!!scope.fbLike ? ' data-href="http://'+window.location.host+'/contents/' + scope.fbLike + '"' : '') + ' data-layout="button_count" data-action="like" data-show-faces="true" data-share="true" data-colorscheme="dark"></div>');
                        $window.FB.XFBML.parse(element.parent()[0]);
                    }
                }
            }
        };
    }
])
