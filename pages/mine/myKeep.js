
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

  /**
   * 生命周期函数--监听页面加载
   */
  onLoad: function (options) {
    //加载数据
    var that = this
    wx.request({
      url: app.apiUrl + '/api/collectArtworks/list?openId=' + app.openId +"&orgId=" + app.orgId,
      success: function (e) {
        if (e.data.code == '0') {
          that.setData({
            list: e.data.data||[]
          })
        }
      }
    })
  },
  sortClick() {
    this.setData({
      hot: 1
    })
  }
})

