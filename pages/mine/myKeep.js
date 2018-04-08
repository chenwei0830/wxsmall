// pages/mine/myKeep.js
// const lp = require("../common/load-page.js")
// let catScrollLeft = 0

// lp({
//     url: "接口地址",
//     data: {
//         distance: 25,
//         loading: false,
//         cid: -1,
//         category: [
//             {
//                 id: 0,
//                 name: "全部",
//                 content: [{
//                     id: 0,
//                     image: 'http://oj1itelvn.bkt.clouddn.com/test.jpg',
//                     title: '魔幻世界&镜花缘',
//                     intro: '行的（面向大众而设计的），转瞬即逝 的（短期方案），可随意消耗的（易忘 的），廉价的，批量生产的阿克江地方哈手机款到发货阿卡丽时代峰峻，奥斯卡绝代风华萨克绝代风华',
//                     date: '09-13',
//                     time: '09:42',
//                     is_comment: 0,
//                     is_like: 0
//                 }]
//             },
//             {
//                 id: 1,
//                 name: "关注",
//                 content: [{
//                     id: 1,
//                     image: 'http://oj1itelvn.bkt.clouddn.com/hhh.jpg',
//                     title: '简约而不简单',
//                     intro: '行的（面向大众而设计的），转瞬即逝 的（短期方案），可随意消耗的（易忘 的），廉价的，批量生产的阿克江地方哈手机款到发货阿卡丽时代峰峻，奥斯卡绝代风华萨克绝代风华',
//                     date: '09-13',
//                     time: '09:42',
//                     is_comment: 0,
//                     is_like: 1
//                 }]
//             },
//             {
//                 id: 2,
//                 name: "摄影"
//             },
//             {
//                 id: 3,
//                 name: "美术"
//             },
//             {
//                 id: 4,
//                 name: "书法"
//             },
//             {
//                 id: 5,
//                 name: "文学"
//             },
//             {
//                 id: 6,
//                 name: "文学"
//             },
//             {
//                 id: 7,
//                 name: "文学"
//             },

//         ],
//         talk: false
//     },
//     methods: {
//         onStart(evt) {
//             const { category } = this.data
//             const { index } = evt.currentTarget.dataset
//             if (!this.data.category[index]) {
//                 return
//             }
//             category.forEach((o, ind) => {
//                 index != ind && (o.pos = 0)
//             })
//             this.setData({ category })
//             const [touch] = evt.touches
//             this['pos' + index] = touch.pageX
//         },
//         onMove(evt) {
//             const { category, distance } = this.data
//             const { index } = evt.currentTarget.dataset
//             if (!this.data.category[index]) {
//                 return
//             }
//             const [touch] = evt.touches
//             let dis = touch.pageX - this['pos' + index]
//             if (dis >= 0) {
//                 dis = 0
//             }
//             if (dis <= -distance) {
//                 dis = -distance
//             }

//             this.setData({
//                 [`category[${index}].pos`]: dis
//             })
//         },
//         onEnd(evt) {
//             const { category, distance } = this.data
//             const { index } = evt.currentTarget.dataset
//             if (!this.data.category[index]) {
//                 return
//             }
//             this.setData({
//                 [`category[${index}].pos`]: category[index].pos < -distance / 2 ? -distance : 0
//             })
//         },
//         clickCategory(evt) {
//             if (this.data.cid == evt.currentTarget.dataset.id) {
//                 return
//             }
//             wx.showLoading({
//                 title: '加载中...',
//             })
//             this.setData({
//                 cid: evt.currentTarget.dataset.id,
//             })

//             //请求数据，提供接口
//             setTimeout(() => {
//                 wx.hideLoading()
//             }, 1000)
//             if (wx.createSelectorQuery) {
//                 const query = wx.createSelectorQuery()
//                 query.select("#cat" + this.data.cid).boundingClientRect()
//                 query.select("#cat").boundingClientRect()
//                 query.exec(res => {
//                     let scrollX = res[0].scrollLeft
//                     this.setData({
//                         tag: {
//                             left: res[0].left + catScrollLeft + 15,
//                             width: res[0].width - 30,
//                         },
//                         scrollX
//                     })
//                 })
//             }
//         },

//         onCatScroll(e) {
//             catScrollLeft = e.detail.scrollLeft
//         },

//         onReady() {
//             this.clickCategory({
//                 currentTarget: {
//                     dataset: {
//                         id: 0
//                     }
//                 }
//             })
//         },

//         commentClick(evt) {
//             //此处请求评论列表的接口
//             const { pos } = evt.currentTarget.dataset
//             const { is_comment } = this.data.category[pos[0]].content[pos[1]]
//             this.setData({
//                 [`category[${pos[0]}].content[${pos[1]}].is_comment`]: !is_comment
//             })
//         },
//         delClick(evt){
//             const { category } = this.data
//             const { index } = evt.currentTarget.dataset
//             // 请求接口
//             wx.showModal({
//                 title: '提示',
//                 content: '确定删除这条收藏吗？',
//                 success: res => {
//                     if (res.confirm) {
//                         console.log('用户点击确定')
//                         category.splice(index, 1)
//                         this.setData({
//                             category
//                         })
//                     } else if (res.cancel) {
//                         console.log('用户点击取消')
//                     }
//                 }
//             })
//         }
//     }
// })

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
            list: e.data.data
          })
        }
        console.log(that.data.list)
      }
    })
  },
  sortClick() {
    this.setData({
      hot: 1
    })
  }
})

