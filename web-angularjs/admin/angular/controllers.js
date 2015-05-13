'use strict';

/* Controllers */



var appControllers = angular.module('appControllers', []);

appControllers.controller('AuthController', ['$rootScope', '$window', '$scope', '$location', 'Resources', function($rootScope, $window, $scope, $location, Resources) {
  $rootScope.$on('event:auth-loginRequired', function() {
    $location.path('/login');
  });

  // $rootScope.$on('event:notFound', function() {
  //   console.log("ro rang vao day")
  //   $window.location.href = 'http://cenrus.com/#notfound';
  // });
  
  $rootScope.$on('event:notFoundToken', function() {
    $location.path('/login');
  });
}]);

appControllers.controller('LoginCtrl', ['$scope', '$rootScope', 'UserInfo', 'Resources', function($scope, $rootScope, UserInfo, Resources) {
  $scope.login = {};

  $rootScope.$on('userLogin', function(event) {
    //$location.path('/profiles'); 
  });

  $scope.ACT = {
    signIn: function() {
      UserInfo['login']($scope.login);
    },
    signOut: function() {
      UserInfo['logout']();
    }
  }
}]);


appControllers.controller('DashboardCtrl', ['$scope', 'Resources', function($scope, Resources) {
  
}]);

appControllers.controller('SettingsCtrl', ['$scope', '$routeParams', 'Resources', function($scope, $routeParams, Resources) {
  $scope.hub = {};

  $scope.editorOptions = {
    focus: true,
    minHeight: 500, 
    maxHeight: 600,
    onImageUpload: function(files, editor, welEditable) {
      var $dialog = $('.note-image-dialog.modal.in');
      sendFile(files[0],editor,welEditable, $dialog);
      $dialog.find('h5.label-image-url').hide();
      $dialog.find('input.note-image-url').hide();
      $dialog.find('.modal-footer').hide();
    },
    onAfterImageInsert: function(url, editor) {
      var $dialog = $('.note-image-dialog.modal.in');
      console.log("ro rang da vao day, onAfterImageInsert");
      $dialog.modal('hide');
      $dialog.find('h5.label-image-url').show();
      $dialog.find('input.note-image-url').show();
      $dialog.find('.modal-footer').show();
    }
  };

  function sendFile(file, editor, welEditable, $dialog) {
    var data = new FormData();
    data.append("file", file);
    console.log('image upload:', file, editor, welEditable);
    console.log(data);
    $.ajax({
        data: data,
        type: "POST",
        url: "http://localhost:9001/media/image/upload?oauth_token=" + $.cookie('user-accesstoken'),
        cache: false,
        contentType: false,
        processData: false,
        success: function(response) {
          console.log("response is: ", response);
          var url = response.filename;
          var width = response.width;
          var height = response.height;
          //console.log("success upload image with url: ", url);
          editor.insertImage(welEditable, "http://localhost:9001/assets/images/" + url);
        }
    });
  }

  $scope.publish = function() {
    $scope.hub.tokenId = $routeParams.token;
    
    Resources['hub']().update($scope.hub, function(response){});
  }
}]);

appControllers.controller('ContentCtrl', ['$scope', 'Resources', 'ContentService', function($scope, Resources, ContentService) {
  $scope.filterFields = ['title', 'score', 'voteUp', 'voteDown', 'creator.display', 'commentCount'];

  $scope.view = {
    sort: 'title',
    sortOrder: 'ASC',
    start: 0,
    size: 20,
    page: 0
  }
  
  $scope.$service = ContentService;
  $scope.$service.getList($scope.view);
}]);

appControllers.controller('CommentCtrl', ['$scope', 'Resources', 'CommentService', function($scope, Resources, CommentService) {
  $scope.filterFields = ['description', 'name'];

  $scope.view = {
    sort: 'user_id',
    sortOrder: 'ASC',
    start: 0,
    size: 20,
    page: 0
  }
  
  $scope.$service = CommentService;
  $scope.$service.getList($scope.view);
}]);

appControllers.controller('TagCtrl', ['$scope', 'Resources', 'TagService', function($scope, Resources, TagService) {
  $scope.input = {
    tag: {}
  }

  $scope.filterFields = ['description', 'name'];

  $scope.view = {
    sort: 'name',
    sortOrder: 'ASC',
    start: 0,
    size: 20,
    page: 0
  }
  
  $scope.$service = TagService;
  $scope.$service.getList($scope.view);
}]);