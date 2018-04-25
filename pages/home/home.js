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
    //获取分类
    this.initCategoryList()
  },
  onReady: function () {
    var that = this
    wx.showLoading({
      title: '',
    })
    that.loadData(that)
    wx.hideLoading()
  },
  onShow: function () {
    this.setData({
      'tabbar.list[3].msg': 2
    })
  },
  initCategoryList: function () {
    var that = this
    wx.request({
      url: app.apiUrl + '/api/getCategoryList',
      success: function (res) {
        that.setData({
          categoryList: res.data.data
        })
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
      }, complete: function () { }
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
    const { current } = this.data
    const id = this.data.category[current]
    
    // cate.total = 2
    // cate.page = 1
    // cate.content = []
    // this.setData({
    //   [`category[${current}]`]: cate
    // })
    // this.loadData()
  },


  //收藏
  keepClick(evt) {
    //此处请求接口
    const { pos } = evt.currentTarget.dataset
    const { is_keep } = this.data.category[pos[0]].content[pos[1]]

    var collectObj = {}
    collectObj.openId = 'o7tbx0KPXyVui_VUg9YgK4UauIWc'
    collectObj.artWorksId = '828d882ea22347c6801c375c0d6b1509'
    collectObj.orgId = app.orgId
    if (is_keep === 0 || !is_keep) {//收藏
      collectObj.type = 1
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
          console.error(' 收藏异常: ' + error);
        },
        complete: function () {
          wx.hideLoading()
        }
      })
    } else {//取消收藏
      collectObj.type = 0
      wx.request({
        url: app.apiUrl + '/api/collectArtworks',
        data: JSON.stringify(collectObj),
        dataType: 'json',
        method: 'POST',
        success: function (res) {
          if (res.data.code == '0') {
            console.log('取消收藏成功')
          } else {
            console.log('取消收藏失败')
          }

        },
        fail: function (error) {
          console.error('取消收藏异常: ' + error);
        },
        complete: function () {
          wx.hideLoading()
        }
      })
    }
    this.setData({
      [`category[${pos[0]}].content[${pos[1]}].is_keep`]: !is_keep
    })
  },
  commentClick(evt) {
    //此处请求评论列表的接口
    const { pos } = evt.currentTarget.dataset
    const { is_comment } = this.data.category[pos[0]].content[pos[1]]
    this.setData({
      [`category[${pos[0]}].content[${pos[1]}].is_comment`]: !is_comment
    })
  },

  likeComment(evt) {
    const [x, y, z] = evt.currentTarget.dataset.pos
    console.log(x, y, z)
    this.setData({
      [`category[${x}].content[${y}].commentList[${z}].isLike`]: !this.data.category[x].content[y].commentList[z].isLike
    })
  },
  //点赞
  likeClick(evt) {
    //此处请求接口
    const { pos } = evt.currentTarget.dataset
    const { is_like } = this.data.category[pos[0]].content[pos[1]]
    this.setData({
      [`category[${pos[0]}].content[${pos[1]}].is_like`]: !is_like
    })
  },
  onShareAppMessage() {
    return {
      title: "西南艺术馆",//分享名称
    }
  }
})