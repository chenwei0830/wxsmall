const app = getApp()
Page({

    /**
     * 页面的初始数据
     */
    data: {
      newsList:[]
    },

    /**
     * 生命周期函数--监听页面加载
     */
    onLoad: function (options) {
        getApp().editTabBar()
        var that = this
        wx.request({
          url: app.apiUrl + '/api/getNewsList',
          success: function (res) {
            that.setData({
              newsList: res.data.data
            })
          },
          fail: function (error) {
            console.error('获取资讯...: ' + error);
          },
          complete: function () {
            wx.hideLoading()
          }
        })

    }
})