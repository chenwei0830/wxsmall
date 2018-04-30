var util = require('../../utils/util.js');
let catScrollLeft = 0
const app = getApp()
Page({
  data: {
    current: 0,
    categoryList: [],
    contentObj: []  //已加载内容
  },
  onLoad: function (options) {
    getApp().editTabBar()
  },
  onShow: function () {
    this.setData({
      'tabbar.list[3].msg': 2
    })
    wx.showLoading({
      title: '',
    })
    //获取分类
    this.initCategoryList()
    
  },
  initCategoryList: function () {
    var that = this
    wx.request({
      url: app.apiUrl + '/api/getCategoryList',
      success: function (res) {
        that.setData({
          categoryList: res.data.data
        })
        that.loadData(that)
      },
      fail: function (error) { },
      complete: function () { }
    })
  },
  //加载数据
  loadData: function (that) {
    const { current } = that.data
    var curentObj = that.data.contentObj[current]
    if (curentObj == undefined) {
      curentObj = {}
      curentObj.hasMoreData = true
      curentObj.searchDate = util.formatTime(new Date());
      curentObj.artTypeParam = that.data.categoryList[current].id
      curentObj.pageNo = 1
      curentObj.pageSize = 5
      curentObj.artWorksList = []
      that.setData({
        [`contentObj[${current}]`]: curentObj
      })
    }else{
      if (curentObj.hasMoreData) {
        that.setData({
          [`contentObj[${current}].pageNo`]: curentObj.pageNo + 1
        })
      }
    }
    if (!curentObj.hasMoreData) {
      return
    }
    var searchObj = {}
    searchObj.searchDateStr = that.data.searchDate;
    searchObj.artTypeParam = that.data.categoryList[current].id
    searchObj.pageNo = that.data.contentObj[current].pageNo
    searchObj.pageSize = that.data.contentObj[current].pageSize
    searchObj.openId = app.openId
    wx.request({
      url: app.apiUrl + '/api/listHome',
      header: {
        'content-type': 'application/json'
      },
      data: JSON.stringify(searchObj),
      dataType: 'json',
      method: 'POST',
      success: function (res) {
        if (res.data.code == '0') {
          var artWorksList = res.data.data
          if (artWorksList != undefined && artWorksList.length > 0) {
            var currentArwWorksList = that.data.contentObj[current].artWorksList.concat(artWorksList)
            that.setData({
              [`contentObj[${current}].artWorksList`]: currentArwWorksList,
              [`contentObj[${current}].pageNo`]: that.data.contentObj[current].pageNo + 1,
              [`contentObj[${current}].hasMoreData`]: artWorksList.length < that.data.contentObj[current].pageSize ? false : true
            })
          } else {
            that.setData({
              [`contentObj[${current}].hasMoreData`]: false
            })
          }
        }
      },
      fail: function (error) {
        console.error('获取数据失败...: ' + error);
      }, complete: function () { wx.hideLoading() }
    })
  },
  //分类点击事件
  clickCategory: function (evt) {
    const { current } = evt.currentTarget.dataset
    if (this.data.current == current) {
      return
    }
    this.setData({
      current,
    })
    this.loadData(this)
    if (wx.createSelectorQuery) {
      const query = wx.createSelectorQuery()
      // query.select("#cat" + this.data.current).boundingClientRect()
      query.selectViewport().scrollOffset
      query.exec(res => {
        let scrollX = res[0].scrollLeft
        this.setData({
          tag: {
            left: res[0].left + catScrollLeft + 15,
            width: res[0].width - 30,
          },
          scrollX
        })
      })
    }
  },
  onCatScroll: function (e) {
    catScrollLeft = e.detail.scrollLeft
  },
  onReachBottom: function () {
    //上拉加载
    this.loadData(this)
  },
  onPullDownRefresh: function () {
    wx.showNavigationBarLoading()
    //下拉刷新
    // const { current } = this.data
    // const id = this.data.category[current]
    
    // cate.total = 2
    // cate.page = 1
    // cate.content = []
    // this.setData({
    //   [`category[${current}]`]: cate
    // })
    // this.loadData()
    wx.hideNavigationBarLoading()
    wx.stopPullDownRefresh()
  },
  //收藏
  keepClick:function(evt) {
    //此处请求接口
    const { index } = evt.currentTarget.dataset
    const { current } = this.data
    this.setData({
      [`contentObj[${current}].artWorksList[${index}].hasCollected`]: this.data.contentObj[current].artWorksList[index].hasCollected > 0 ? 0 : 1
    })
    //保存或取消关注
    var collectObj = {}
    collectObj.openId = app.openId
    collectObj.orgId = app.orgId
    collectObj.artWorksId = this.data.contentObj[current].artWorksList[index].id
    collectObj.type = this.data.contentObj[current].artWorksList[index].hasCollected
    
    wx.request({
      url: app.apiUrl + '/api/collectArtworks',
      data: JSON.stringify(collectObj),
      dataType: 'json',
      method: 'POST',
      success: function (res) {
        if (res.data.code == '0') {
          console.log('收藏成功')
        } else {
          console.log('收藏失败')
        }

      },
      fail: function (error) {
        console.error(' 系统异常: ' + error);
      },
      complete: function () { }
    })
  },
  //评论点赞
  likeComment:function(evt) {
  },
  //点赞
  likeClick:function(evt) {
    const { index } = evt.currentTarget.dataset
    const { current } = this.data
    this.setData({
      [`contentObj[${current}].artWorksList[${index}].hasDz`]: this.data.contentObj[current].artWorksList[index].hasDz > 0 ? 0 : 1,
      [`contentObj[${current}].artWorksList[${index}].dzNum`]: this.data.contentObj[current].artWorksList[index].hasDz > 0 ? this.data.contentObj[current].artWorksList[index].dzNum - 1 : this.data.contentObj[current].artWorksList[index].dzNum + 1
    })
    
    //保存或取消点赞
    var dzObj = {}
    dzObj.openId = app.openId
    dzObj.orgId = app.orgId
    dzObj.targetId = this.data.contentObj[current].artWorksList[index].id
    dzObj.type = '0' //0-作品 1-评论
    dzObj.delFlag = this.data.contentObj[current].artWorksList[index].hasDz > 0 ? '0' : '1'
    
    wx.request({
      url: app.apiUrl + '/api/targetDz',
      data: JSON.stringify(dzObj),
      dataType: 'json',
      method: 'POST',
      success: function (res) {
        if (res.data.code == '0') {
          console.log('点赞或取消点赞成功')
        } else {
          console.log('点赞或取消点赞失败')
        }
      },
      fail: function (error) {
        console.error(' 系统异常: ' + error);
      },
      complete: function () { }
    })
  },
  onShareAppMessage() {
    return {
      title: "西南艺术馆",//分享名称
    }
  }
})