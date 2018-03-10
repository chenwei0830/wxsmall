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
    var openid = wx.getStorageInfoSync("openid") || []
    var that = this
    if (openid){
      //缓存中未获取到openId,则重新获取
      wx.login({
        success: function(res){
          console.log("登录时得到的code----" + res.code)
          if(res.code){
            //根据code获取openId
            wx.request({
              url: that.apiUrl + '/api/getOpenId',
              data: {
                jsCode: res.code
              },
              success: function (res) {

                console.log("获取openId----" + JSON.stringify(res.data.data));
                that.openId = res.data.data.openid //赋值全局
                wx.setStorageSync('openid', res.data.data.openid) //放入缓存
              },
              fail: function (error) {
                console.error('获取openId失败...: ' + error);
              }
            })
          }
        }
      })    
    }else{
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
  apiUrl: 'http://localhost:8080/sourthArtSys',//接口地址
  user: null,//用户信息
  openId: null,//用户唯一标识
  requireHttp() {
    const ps = getCurrentPages()
    const current = ps[ps.length - 1]
    console.warn(`******此处应当请求接口，页面：${ps[ps.length-1].route}******`)
  }
})