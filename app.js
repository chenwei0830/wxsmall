//app.js
const wxToast = require("./utils/wx-toast");
const tabbar = {
  color: "#353535",
  selectedColor: "#03b9bb",
  backgroundColor: "#f7f7fa",
  borderStyle: "#ccc",
  list: [
    {
      pagePath: "/pages/home/home",
      text: "首页",
      iconPath: "/assets/icon/nav-home.png",
      selectedIconPath: "/assets/icon/nav-home2.png",
      selected: true
    },
    {
      pagePath: "/pages/art/art-list",
      text: "艺术名片",
      iconPath: "/assets/icon/nav-art.png",
      selectedIconPath: "/assets/icon/nav-art2.png",
      selected: false
    },
    {
      pagePath: "/pages/fb/index",
      iconPath: "/assets/icon/nav-fb.png",
      selectedIconPath: "/assets/icon/nav-fb.png",
      selected: false,
      size: 'big'
    },
    {
      pagePath: "/pages/msg/msg",
      text: "本馆资讯",
      iconPath: "/assets/icon/nav-talk.png",
      selectedIconPath: "/assets/icon/nav-talk2.png",
      selected: false,
      msg: '0'
    },
    {
      pagePath: "/pages/mine/mine",
      text: "我的",
      iconPath: "/assets/icon/nav-mine.png",
      selectedIconPath: "/assets/icon/nav-mine2.png",
      selected: false
    }
  ],
  position: "bottom"
}
App({
  onLaunch: function () {
    var that = this
    //缓存中未获取到openId,则重新获取
    var openId = wx.getStorageSync('openid')||''
    if (openId==''){
      wx.login({
        success: function (res) {
          if (res.code) {
            //根据code获取openId
            wx.request({
              url: that.apiUrl + '/api/getOpenId',
              data: {
                jsCode: res.code
              },
              success: function (res) {
                if (res.data.data.openid == undefined) {
                  console.log("获取openId失败");
                } else {
                  that.openId = res.data.data.openid //赋值全局
                  wx.setStorageSync('openid', res.data.data.openid) //放入缓存
                }

              },
              fail: function (error) {
                console.error('获取openId异常...: ' + error);
              }
            })
          }
        }
      })
    }else{
      that.openId = openId
    }
    
    wx.getUserInfo({
      success: info => {
        //将用户信息全局保存
        that.user = info.userInfo
        var userObj = {}
        userObj.openId = that.openId
        userObj.orgId = that.orgId
        userObj.nickName = info.userInfo.nickName
        userObj.photo = info.userInfo.avatarUrl
        userObj.gender = info.userInfo.gender
        userObj.city = info.userInfo.city
        userObj.province = info.userInfo.province
        userObj.country = info.userInfo.country
        userObj.language = info.userInfo.language

        wx.request({
          url: that.apiUrl + '/api/login',
          data: JSON.stringify(userObj),
          dataType: 'json',
          method: 'POST',
          success: function (res) {
            console.log(res.data)
            if (res.data.code == '0') {
              //登录后覆盖原用户信息
              that.user.artType = res.data.data.artType
              that.user.artLevel = res.data.data.artLevel
              console.log('登录成功')
            } else {
              console.log('登录失败')
            }
          },
          fail: function (error) {
            console.error(' 登录异常: ' + error);
          },
          complete: function () {
          }
        })
      }
    })
  },
  wxToast,
  editTabBar: function () {
    const ps = getCurrentPages()
    const current = ps[ps.length - 1]
    const route = "/" + current.route
    tabbar.list.forEach(o => {
      o.selected = route === o.pagePath
    })
    current.setData({
      tabbar
    })
  },
  apiUrl: 'https://www.chenqimao.com/sourthArtSys',//接口地址 http://localhost:8080/sourthArtSys
  user: null,//用户信息
  openId: '',//用户唯一标识  
  orgId: '2',//组织机构ID

  requireHttp() {
    const ps = getCurrentPages()
    const current = ps[ps.length - 1]
    console.warn(`******此处应当请求接口，页面：${ps[ps.length - 1].route}******`)
  }
})