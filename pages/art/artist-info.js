const app = getApp()
Page({
  data: {
    artObj: {}
  },
  onLoad: function (options) {
    wx.showLoading({
      title: '',
    })
    var that = this
    wx.request({
      url: app.apiUrl + '/api/getArtInfo?id='+options.id,
      success: function (res) {
        that.setData({
          artObj: res.data.data
        })
        console.log(res.data.data)
      },
      fail: function (error) {
        console.error('获取艺术家信息...: ' + error);
      },
      complete: function () {
        wx.hideLoading()
      }
    })
  }

})
