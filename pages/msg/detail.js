const app = getApp()
Page({

    /**
     * 页面的初始数据
     */
    data: {
      newsObj:{}
    },

    /**
     * 生命周期函数--监听页面加载
     */
    onLoad: function (options) {
      wx.showLoading({
        title: '',
      })
      var that = this
      wx.request({
        url: app.apiUrl + '/api/getNewsInfo?newsId=' + options.id,
        success: function (res) {
          console.log(res.data.data)
          that.setData({
            newsObj: res.data.data
          })
        },
        fail: function (error) {
          console.error('获取资讯详情失败...: ' + error);
        },
        complete: function () {
          wx.hideLoading()
        }
      })
    }
})