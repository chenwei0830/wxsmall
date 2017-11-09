//index.js
//获取应用实例
const app = getApp()

Page({
  data: {
    grids: [
      {
        iconId:'btn-font',
        btnName: '文字',
        btnIcon: '../../icons/btn-font.png'
      },
      {
        iconId: 'btn-photo',
        btnName: '照片',
        btnIcon: '../../icons/btn-photo.png'
      },
      {
        iconId: 'btn-video',
        btnName: '视频',
        btnIcon: '../../icons/btn-video.png'
      }
    ]
  },
  onGoTextPage : function(ev){//文本编辑页面
    wx.navigateTo({
      url: '../release/textpage/textpage'
    });
  },
  onGoPhotoPage: function (ev) {//图片编辑页面
    wx.chooseImage({
      count: 1, // 默认9
      sizeType: ['original', 'compressed'], // 可以指定是原图还是压缩图，默认二者都有
      sourceType: ['album', 'camera'], // 可以指定来源是相册还是相机，默认二者都有
      success: function (res) {
        // 返回选定照片的本地文件路径列表，tempFilePath可以作为img标签的src属性显示图片
        var tempFilePaths = res.tempFilePaths;
        console.log(tempFilePaths);
      }
    })
  },
  onGoVideoPage: function (ev) {//视频编辑页面
    var that = this
    wx.chooseVideo({
      sourceType: ['album', 'camera'],
      maxDuration: 60,
      camera: 'back',
      success: function (res) {
        console.log(res.tempFilePath);
      }
    })
  }

})
