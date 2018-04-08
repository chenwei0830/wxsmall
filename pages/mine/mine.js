// pages/mine/mine.js
const app = getApp()
Page({

  /**
   * 页面的初始数据
   */
  data: {
    advice: false,
    mineObj:{
      authCount:0,                //认证数
      artWorksCount: 0,           //作品总数
      newArtWorkCount:0,          //新作品数
      collectCount: 0,            //收藏作品数
      interestArtCount: 0         //关注艺术家数
    }
  },

  /**
   * 生命周期函数--监听页面加载
   */
  onLoad: function (options) {
    console.log(app.user)
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
    //接口获取初始信息
    var that = this
    wx.request({
      url: app.apiUrl + '/api/mine?openId='+app.openId,
      success: function(e){
        if(e.data.code=='0'){
            that.setData({
              mineObj: e.data.data
            })
        }
        console.log(that.data.mineObj)
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