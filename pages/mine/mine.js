//获取应用实例
const app = getApp();

Page({
  data:{
      headImage:'../../images/head.png',
      userName:'李敏',
      isArt:true,
      artLevel:"省级艺术家",
      artIcon:"../../icons/art-sj.png",
      list: [
        {
          id: 'form',
          name: '表单',
          open: false,
          pages: ['button', 'list', 'input', 'slider', 'uploader']
        },
        {
          id: 'widget',
          name: '基础组件',
          open: false,
          pages: ['article', 'badge', 'flex', 'footer', 'gallery', 'grid', 'icons', 'loadmore', 'panel', 'preview', 'progress']
        },
        {
          id: 'feedback',
          name: '操作反馈',
          open: false,
          pages: ['actionsheet', 'dialog', 'msg', 'picker', 'toast']
        },
        {
          id: 'nav',
          name: '导航相关',
          open: false,
          pages: ['navbar', 'tabbar']
        },
        {
          id: 'search',
          name: '搜索相关',
          open: false,
          pages: ['searchbar']
        }
      ]
  },
  //事件处理函数
  bindViewTap: function () {
    wx.navigateTo({
      url: '../logs/logs'
    })
  },
  onLoad: function () {
  }
})
