const app = getApp()
Page({
  data: {
    artList:[]
  },
  onLoad: function (options) {
    wx.showLoading({
      title: '',
    })
    getApp().editTabBar()
    var that = this
    wx.request({
      url: app.apiUrl + '/api/getArtList',
      success: function (res) {
        that.setData({
          artList: res.data.data
        })
      },
      fail: function (error) {
        console.error('获取艺术名片失败...: ' + error);
      },
      complete: function(){
        wx.hideLoading()
      }
    })
  }

})
