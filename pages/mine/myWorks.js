const app = getApp()
let catScrollLeft = 0

Page({

  /**
   * 页面的初始数据
   */
  data: {
    loading: false,
    list: [],
    cid: -1,
    hot: 0,
    time: 1
  },
  sortClick() {
    this.setData({
      hot: 1
    })
  },
  onShow: function (){
    //加载数据
    var that = this
    wx.request({
      url: app.apiUrl + '/api/mine/artworks/list?openId=' + app.openId,
      success: function (e) {
        if (e.data.code == '0') {
          that.setData({
            list: e.data.data
          })
        }
        console.log(that.data.list)
      }
    })
  }
})