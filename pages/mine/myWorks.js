// pages/mine/myWorks.js
let catScrollLeft = 0

Page({

  /**
   * 页面的初始数据
   */
  data: {
    loading: false,
    list:[],
    cid: -1,
    category: [
      {
        id: 0,
        name: "全部",
        content: [{
          id: 0,
          image: 'http://oj1itelvn.bkt.clouddn.com/test.jpg',
          title: '魔幻世界&镜花缘',
          intro: '行的（面向大众而设计的），转瞬即逝 的（短期方案），可随意消耗的（易忘 的），廉价的，批量生产的阿克江地方哈手机款到发货阿卡丽时代峰峻，奥斯卡绝代风华萨克绝代风华',
          date: '09-13',
          time: '09:42',
          is_comment: 0,
          is_like: 0
        }]
      },
      {
        id: 1,
        name: "关注",
        content: [{
          id: 1,
          image: 'http://oj1itelvn.bkt.clouddn.com/hhh.jpg',
          title: '简约而不简单',
          intro: '行的（面向大众而设计的），转瞬即逝 的（短期方案），可随意消耗的（易忘 的），廉价的，批量生产的阿克江地方哈手机款到发货阿卡丽时代峰峻，奥斯卡绝代风华萨克绝代风华',
          date: '09-13',
          time: '09:42',
          is_comment: 0,
          is_like: 1
        }]
      },
    ],
    hot:0,
    time:1
  },

  /**
   * 生命周期函数--监听页面加载
   */
  onLoad: function (options) {
     
  },
    sortClick(){
        this.setData({
            hot:1
        })
    }
})