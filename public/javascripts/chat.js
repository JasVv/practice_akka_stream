function main() {
  const vm = new Vue({
    el: '#app',
    data: function() {
      return {
        id: new Date().getTime().toString(16)  + Math.floor(1000*Math.random()).toString(16),
        name: "",
        text: "",
        room: "A",
        messages: [],
        websocket: null,
        turn: true,
      }
    },

    methods: {
      pushMessage: function(id, name, text) {
        this.messages.push({
          "id": id,
          "name": name,
          "text": text,
        });
      },

      entryRoom : function() {
        this.websocket = "aaa";

        console.log(this.websocket);
      },

      sendMessage : function() {
        if (this.websocket) {
          if (this.turn) {
            this.pushMessage(this.id, this.name, this.text);
          } else {
            this.pushMessage("aaa", this.name, this.text);
          }
          this.turn = !this.turn;
          this.text = "";
        };
      },

      exitRoom : function() {
        this.websocket = null;
        this.text = "";
        this.clearMessage();
      },

      clearMessage : function() {
        this.messages = [];
      }
    }
  });
};
