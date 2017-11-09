//index.js
//获取应用实例
const app = getApp()

Page({
  data: {
    imageList: [
      '../../images/b-2.jpg',
      '../../images/b-3.jpg',
      '../../images/b-4.jpg',
      '../../images/b-5.jpg',
      '../../images/b-5.jpg'
    ],
    grids: [
      {
        menuName:'文学',
        bgImage:'http://7xjhv4.com1.z0.glb.clouddn.com/sourceArt/flwx.png'
      },
      {
        menuName: '美术',
        bgImage: 'http://7xjhv4.com1.z0.glb.clouddn.com/sourceArt/flmx.png'
      },
      {
        menuName: '书法',
        bgImage: 'http://7xjhv4.com1.z0.glb.clouddn.com/sourceArt/flmb.png'
      },
      {
        menuName: '摄影',
        bgImage: 'http://7xjhv4.com1.z0.glb.clouddn.com/sourceArt/flsy.png'
      }
    ]
  }
})
