Page({
  data: {
    tabs: ["推荐", "文学", "美术","书法","摄影"],
    activeIndex: 0,
    sliderOffset: 0,
    sliderLeft: 0,
    boxList: [
      {
        id:"box_1",
        headImg:"../../images/head.png",
        nickName:"萌萌的江流儿",
        isArt:false,
        artLevel:"",
        createTime:"10/21 19:45",
        readNum:"6.5万",
        title:"标题一",
        context:"有各种物质组成的巨型球状天梯，叫做星球。星球有一定的形状，有自己的运行轨道。",
        contextImg:"../../images/zp-2.jpg",
        dzNum:"2003",
        plNum:"324",
        fxNum:"12",
        plList:[
          {
            id: "box_1_pl_1",
            headImg: "../../images/head.png",
            nickName: "如花一般美丽的男子",
            dzNum: "4680",
            context: "越来越熟了，真不好下手。星球有一定的形状，有自己的运行轨道。"
          }
        ]
      },
      {
        id: "box_2",
        headImg: "../../images/head.png",
        nickName: "很有文采的",
        isArt: true,
        artLevel: "国家级",
        createTime: "10/21 19:45",
        readNum: "4万",
        title: "标题二",
        context: "有各种物质组成的巨型球状天梯，叫做星球。星球有一定的形状，有自己的运行轨道。",
        contextImg: "../../images/zp-1.jpg",
        dzNum: "23",
        plNum: "324",
        fxNum: "12",
        plList: [
          {
            id: "box_2_pl_1",
            headImg: "../../images/head.png",
            nickName: "孤独九妹小跳跳",
            isArt: true,
            artLevel: "国家级",
            dzNum: "23344",
            context: "那个七度空间呢？我开始心疼女生了。星球有一定的形状，有自己的运行轨道。"
          }
        ]
      },
      {
        id: "box_3",
        headImg: "../../images/head.png",
        nickName: "美丽的摄影家",
        isArt: true,
        artLevel: "省级",
        createTime: "10/21 19:45",
        readNum: "6.5万",
        title: "标题三",
        context: "有各种物质组成的巨型球状天梯，叫做星球。星球有一定的形状，有自己的运行轨道。",
        contextImg: "../../images/zp-2.jpg",
        dzNum: "2003",
        plNum: "324",
        fxNum: "12",
        plList: [
          {
            id: "box_3_pl_1",
            headImg: "../../images/head.png",
            nickName: "苍天有井自然空",
            isArt: true,
            artLevel: "国家级",
            dzNum: "2144",
            context: "儿们啊，你们转到了，上王者了。星球有一定的形状，有自己的运行轨道。"
          }
        ]
      }
    ]
  },
  onLoad: function () {
    var that = this;
    wx.getSystemInfo({
      success: function (res) {
        that.setData({
          sliderLeft: res.windowWidth / that.data.tabs.length/2,
          sliderOffset: res.windowWidth / that.data.tabs.length * that.data.activeIndex
        });
      }
    });
  },
  tabClick: function (e) {
    this.setData({
      sliderOffset: e.currentTarget.offsetLeft,
      activeIndex: e.currentTarget.id
    });
  }
});