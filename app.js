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
      pagePath: "/pages/art/artist",
      text: "艺术家",
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
      text: "消息",
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
    var openid = wx.getStorageInfoSync("openid") || ''
    var that = this
    if (openid) {
      //缓存中未获取到openId,则重新获取
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
                if (res.data.data.openid === undefined) {
                  console.log("获取openId失败");
                } else {
                  that.openId = res.data.data.openid //赋值全局
                  wx.setStorageSync('openid', res.data.data.openid) //放入缓存
                  wx.showLoading({
                    title: '加载中...',
                  })
                  wx.getUserInfo({
                    success: info => {
                      //将用户信息全局保存
                      that.user = info.userInfo
                      //登录注册
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
                          if (res.data.code == '0') {
                            console.log('登录成功')
                          } else {
                            console.log('登录失败')
                          }

                        },
                        fail: function (error) {
                          console.error(' 登录异常: ' + error);
                        },
                        complete: function () {
                          wx.hideLoading()
                        }
                      })
                    }
                  })
                }

              },
              fail: function (error) {
                console.error('获取openId失败...: ' + error);
              }
            })
          }
        }
      })
    } else {
      this.openid = openid
    }
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