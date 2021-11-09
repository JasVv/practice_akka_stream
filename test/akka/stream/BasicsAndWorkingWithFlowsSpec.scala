package akka.stream

import akka.NotUsed
import akka.stream.scaladsl._
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.test.Injecting

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

/**
 * Akka Streamの基礎
 *
 * Basics and working with Flows
 * https://akka-ja-2411-translated.netlify.app/scala/stream/stream-flows-and-basics.html
 */
class BasicsAndWorkingWithFlowsSpec extends PlaySpec
  with GuiceOneAppPerSuite
  with Injecting {

  // Graphの実行にはMaterializerが必要
  implicit private val materializer: Materializer = inject[Materializer]

  "Basics and working with Flows" should {
    "SourceとSinkの作成と実行" in {
      // 1 ~ 10までを出力するSource
      // 型パラメータ1つめは出力する値の型、2つめは実行時に返すマテリアライズされた値の型 (NotUsedはマテリアライズされた値を使用しないことを示す.つまり何も返さない)
      val source: Source[Int, NotUsed] = Source(1 to 10)

      // 入力された数値を全て足し、マテリアライズされた値として足した結果を返すSink
      // 型パラメータ1つめは入力される値の型、2つめは実行時に返すマテリアライズされた値の型
      val sink: Sink[Int, Future[Int]] = Sink.fold[Int, Int](0)(_ + _)

      // SourceとSinkを結合してRunnableGraphを作る
      // Keepは結合した2つのGraphのうち、どちら側のマテリアライズされた値を保持するかを示す
      // ここではKeep.rightでsink側のマテリアライズされた値を保持する
      val runnable: RunnableGraph[Future[Int]] = source.toMat(sink)(Keep.right)

      // runnableGraphの実行. 上記のKeep.rightにより、sink側のマテリアライズされた値であるFuture[Int]が返ってくる
      val res1: Future[Int] = runnable.run()
      Await.result(res1, Duration.Inf) mustBe 55

      // source.toMat(sink)(Keep.right)と等価
      val res2: Future[Int] = source.runWith(sink)
      Await.result(res2, Duration.Inf) mustBe 55

      // source.toMat(sink)(Keep.left)と等価
      val res3: NotUsed = sink.runWith(source)
      assert(res3.isInstanceOf[NotUsed])

      // source.toMat(sink)(Keep.left)と等価
      val res4: NotUsed = source.to(sink).run()
      assert(res4.isInstanceOf[NotUsed])
    }

    "Flowの作成と実行" in {
      // Flowは1つの入力と1つの出力を持つ
      // 型パラメータ1つめは入力される値の型、2つめは出力される値の型、3つめは実行時に返すマテリアライズされた値の型
      val flow: Flow[Int, Int, NotUsed] = Flow.fromFunction(_ * 2)
      // Flow[Int].map(_ * 2)も同じ

      // flowは単体では実行できず、実行するときはSourceとSinkを結合する必要がある
      val source: Source[Int, NotUsed] = Source(1 to 10)
      val sink: Sink[Int, Future[Int]] = Sink.fold[Int, Int](0)(_ + _)
      val runnable = source.via(flow).toMat(sink)(Keep.right)

      val res1: Future[Int] = runnable.run()
      Await.result(res1, Duration.Inf) mustBe 110

      // source.via(flow).toMat(sink)(Keep.both).run()と等価
      // Keep.bothなので戻り値はsourceとsinkのマテリアライズされた値のTuple
      val res2 = flow.runWith(source, sink)
      assert(res2._1.isInstanceOf[NotUsed])
      Await.result(res2._2, Duration.Inf) mustBe 110

      Flow[Int].map(_ * 2)
    }

    "タスクの分割" in {
      val source = Source(1 to 5)
      val sink = Sink.fold[Int, Int](0)(_ + _)

      // タスクの分割をしない場合、Streamを1アクター(1CPUコア)で処理するため、A1とB1は並列処理にはならない
      // よって以下の例では A1:B1:A1:B1:A1:B1:A1:B1:A1:B1: のように交互に処理される
      val res1 = source
        .map { i =>
          print("A1:")
          i + 1
        }
        .map { i =>
          print("B1:")
          i * 2
        }
        .toMat(sink)(Keep.right)
        .run()

      Await.result(res1, Duration.Inf) mustBe 40

      println()

      // asyncによって、「asyncまでの処理」と「asyncの後の処理」をそれぞれ別アクター(別CPUコア)で並列処理できる
      // 以下の例では A2とB2はそれぞれ独立して行われるため、print結果は A2:A2:A2:A2:B2:A2:B2:B2:B2:B2: のように、A2とB2が交互に現れない形になる
      val res2 = source
        .map { i =>
          print("A2:")
          i + 1
        }
        .async
        .map { i =>
          print("B2:")
          i * 2
        }
        .toMat(sink)(Keep.right)
        .run()

      Await.result(res2, Duration.Inf) mustBe 40
    }
  }
}
