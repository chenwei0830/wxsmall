// pages/art/search.js
const lp = require("../common/load-page.js")
lp({
    url: "接口地址",
    data: {
        distance: 25
    },
    methods: {
        watch(evt) {
            const { index } = evt.currentTarget.dataset
            this.setData({
                [`list[${index}].is_watch`]: !this.data.list[index].is_watch,
                [`list[${index}].pos`]: 0
            })
            //请求接口
        },

        onStart(evt) {
            const { list } = this.data
            const { index } = evt.currentTarget.dataset
            if (!this.data.list[index].is_watch) {
                return
            }
            list.forEach((o, ind) => {
                index != ind && (o.pos = 0)
            })
            this.setData({ list })
            const [touch] = evt.touches
            this['pos' + index] = touch.pageX
        },
        onMove(evt) {
            const { list, distance } = this.data
            const { index } = evt.currentTarget.dataset
            if (!this.data.list[index].is_watch) {
                return
            }
            const [touch] = evt.touches
            let dis = touch.pageX - this['pos' + index]
            if (dis >= 0) {
                dis = 0
            }
            if (dis <= -distance) {
                dis = -distance
            }

            this.setData({
                [`list[${index}].pos`]: dis
            })
        },
        onEnd(evt) {
            const { list, distance } = this.data
            const { index } = evt.currentTarget.dataset
            if (!this.data.list[index].is_watch) {
                return
            }
            this.setData({
                [`list[${index}].pos`]: list[index].pos < -distance / 2 ? -distance : 0
            })
        }
    }
})
