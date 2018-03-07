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


  requireHttp() {
    const ps = getCurrentPages()
    const current = ps[ps.length - 1]
    console.warn(`******此处应当请求接口，页面：${ps[ps.length-1].route}******`)
  }
})