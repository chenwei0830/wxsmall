// pages/mine/mine.js
const app = getApp()
Page({

  /**
   * 页面的初始数据
   */
  data: {
    advice: false,
    isAuthing: false,//是否等待认证审核
  },

  /**
   * 生命周期函数--监听页面加载
   */
  onLoad: function (options) {
    if (app.user==null) {
      wx.login({
        success: res => {
          wx.getUserInfo({
            success: info => {
              this.setData({
                user: info.userInfo
              })
              //将用户信息全局保存
              app.user = this.data.user
            }
          })
        }
      })
    } else {
      this.setData({
        user: app.user
      })
    }
    //接口获取认证状态
    var that = this
    wx.request({
      url: app.apiUrl + '/api/authStatus?openId='+app.openId,
      success: function(e){
        if(e.data.code=='0'){
          if (e.data.data>0){//审核认证条数大于0，表明正在审核
            that.setData({
              isAuthing: true
            })
          }
        }
      }
    })

    getApp().editTabBar();
  },
  loadData() {
    // 请求接口
  },
  onInput(evt) {
    this.setData({
      content: evt.detail.value
    })
  },
  submit() {
    if (!this.data.content) {
      app.wxToast.error('请输入您的意见或建议~');
      return;
    }
    // 请求接口，提交意见反馈

  },
  adviceClick() {
    this.setData({
      advice: true
    })
  },
  closeClick() {
    this.setData({
      advice: false
    })
  },
  call() {
    wx.makePhoneCall({
      phoneNumber: '18888888888'
    })
  }
})